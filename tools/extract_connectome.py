#!/usr/bin/env python3
"""Extrai um subcircuito de aprendizado do conectoma FlyWire FAFB v783.

Entrada esperada (downloads publicas, CC-BY-4.0):
  nodes.parquet        -> 139.255 neuronios anotados (Schlegel et al. 2024)
  connections.parquet  -> 15.091.983 pares dirigidos >=1 sinapse (FlyWire v783)

Saida:
  app/src/main/assets/connectome/flywire_mb.bin   (asset binario lido pelo app)
  tools/connectome_stats.json                     (relatorio da extracao)

O subcircuito selecionado e o sistema de aprendizado da mosca:
  - Corpos pedunculados (Kenyon cells, MBON, MBIN)
  - Neurônios dopaminergicos (DAN / PAM / PPL1) = recompensa
  - Vias sensoriais de entrada (ALPN, olfatorio, mecanossensorial, gustativo)
  - Complexo central (CX) e neuronios descendentes/motores = saida

Uso:
  python tools/extract_connectome.py \
      --nodes "C:/Users/conta/Downloads/nodes.parquet" \
      --connections "C:/Users/conta/Downloads/connections.parquet"
"""

from __future__ import annotations

import argparse
import json
import math
import os
import random
import struct
import sys
from collections import Counter, defaultdict

import numpy as np
import pyarrow as pa
import pyarrow.compute as pc
import pyarrow.parquet as pq

MAGIC = b"FLYWIRE1"
VERSION = 1

ROLE_INPUT, ROLE_INTER, ROLE_DOPAMINE, ROLE_OUTPUT = 0, 1, 2, 3
NT_ACH, NT_GABA, NT_GLUT, NT_DA, NT_SER, NT_OCT, NT_OTHER = 0, 1, 2, 3, 4, 5, 6

NT_TO_ID = {
    "acetylcholine": NT_ACH,
    "gaba": NT_GABA,
    "glutamate": NT_GLUT,
    "dopamine": NT_DA,
    "serotonin": NT_SER,
    "octopamine": NT_OCT,
}
NT_NAMES = {
    NT_ACH: "acetylcholine",
    NT_GABA: "gaba",
    NT_GLUT: "glutamate",
    NT_DA: "dopamine",
    NT_SER: "serotonin",
    NT_OCT: "octopamine",
    NT_OTHER: "desconhecido",
}
# gaba e glutamate sao inibitorios no SNC de Drosophila; acetilcolina excita.
# dopamina/serotonina/octopamina sao modulatorios: nao propagam potencial.
NT_SIGN = {
    NT_ACH: 1,
    NT_GABA: -1,
    NT_GLUT: -1,
    NT_DA: 0,
    NT_SER: 0,
    NT_OCT: 0,
    NT_OTHER: 0,
}

INPUT_CLASSES = {"ALPN", "TPN", "olfactory", "mechanosensory", "gustatory"}
OUTPUT_CLASSES = {"MBON", "MBIN"}


def log(msg: str) -> None:
    print(f"[extract] {msg}", flush=True)


def pct(count: int, total: int) -> float:
    return round(100.0 * count / total, 2) if total else 0.0


def load_nodes(path: str) -> dict:
    table = pq.read_table(path).to_pydict()
    n = len(table["root_id"])
    log(f"nodes.parquet: {n} neuronios")
    return table


def select_neurons(table: dict, seed: int) -> list[int]:
    """Escolhe os indice dos neuronios do subcircuito, aplicando cotas por tipo."""
    n = len(table["root_id"])
    rng = random.Random(seed)
    picked: list[int] = []
    seen: set[int] = set()

    def take(indices: list[int], quota: int | None, label: str) -> None:
        if not indices:
            return
        rng.shuffle(indices)
        chosen = indices if quota is None else indices[:quota]
        added = 0
        for i in chosen:
            if i not in seen:
                seen.add(i)
                picked.append(i)
                added += 1
        log(f"  {label}: +{added} (candidatos {len(indices)})")

    def where(pred) -> list[int]:
        return [i for i in range(n) if pred(i)]

    cls = table["cell_class"]
    sup = table["super_class"]
    nt = table["top_nt"]

    # Recompensa: neuronios dopaminergicos.
    take(where(lambda i: cls[i] == "DAN"), 331, "DAN (dopamina)")
    # Coracao do aprendizado: corpos pedunculados.
    take(where(lambda i: cls[i] == "Kenyon_Cell"), 3000, "Kenyon cells")
    take(where(lambda i: cls[i] == "MBON"), None, "MBON (saida)")
    take(where(lambda i: cls[i] == "MBIN"), None, "MBIN")
    take(where(lambda i: cls[i] == "TuBu"), 150, "TuBu")
    take(where(lambda i: cls[i] == "mAL"), 77, "mAL")
    # Entradas sensoriais.
    take(where(lambda i: cls[i] == "ALPN"), 300, "ALPN (olfatorio)")
    take(where(lambda i: cls[i] == "olfactory"), 250, "olfatorio")
    take(where(lambda i: cls[i] == "mechanosensory"), 150, "mecanossensorial")
    take(where(lambda i: cls[i] == "gustatory"), 90, "gustativo")
    take(where(lambda i: sup[i] == "visual_projection"), 220, "projecao visual")
    # Associacao / navegacao.
    take(where(lambda i: cls[i] == "CX"), 500, "complexo central")
    take(where(lambda i: cls[i] == "LHLN"), 250, "LHLN")
    take(where(lambda i: cls[i] == "ALLN"), 200, "ALLN")
    take(where(lambda i: cls[i] == "LHCENT"), None, "LHCENT")
    take(where(lambda i: cls[i] == "ALIN"), 24, "ALIN")
    take(where(lambda i: cls[i] == "ALON"), 14, "ALON")
    # Saidas motoras.
    take(where(lambda i: sup[i] == "descending"), 200, "descendentes")
    take(where(lambda i: sup[i] == "motor"), None, "motor")
    take(where(lambda i: sup[i] == "endocrine"), None, "endocrino")
    # Contexto visual.
    take(where(lambda i: sup[i] == "optic"), 250, "optico")
    # Preenchimento com neuronios centrais aleatorios.
    take(
        where(lambda i: sup[i] == "central" and (cls[i] is None or cls[i] not in {"Kenyon_Cell", "DAN"})),
        600,
        "central (outros)",
    )
    return sorted(picked)


def nt_of(table: dict, i: int) -> int:
    """Neurotransmissor usado na simulacao.

    O pacote de anotacoes traz top_nt='dopamine' para as celulas de Kenyon,
    o que e biologicamente incorreto (KCs sao colinergicas). Sem essa correcao
    todas as sinapses de KC ficariam modulatorias e o circuito nao propagaria.
    """
    cls = table["cell_class"][i]
    if cls == "Kenyon_Cell":
        return NT_ACH
    if cls == "DAN":
        return NT_DA
    return NT_TO_ID.get(table["top_nt"][i], NT_OTHER)


def role_of(table: dict, i: int) -> int:
    cls = table["cell_class"][i]
    sup = table["super_class"][i]
    if cls == "DAN":
        return ROLE_DOPAMINE
    if cls in INPUT_CLASSES or sup == "visual_projection":
        return ROLE_INPUT
    if cls in OUTPUT_CLASSES or sup in {"descending", "motor", "endocrine"}:
        return ROLE_OUTPUT
    return ROLE_INTER


def collect_edges(connections_path: str, ids: set[int], min_syn: int, batch_size: int = 1_000_000):
    """Le connections.parquet em blocos e mantem apenas arestas internas ao subcircuito."""
    pf = pq.ParquetFile(connections_path)
    value_set = pa.array(sorted(ids), type=pa.int64())
    pres: list[np.ndarray] = []
    posts: list[np.ndarray] = []
    syns: list[np.ndarray] = []
    total_rows = 0
    kept = 0
    next_log = 5_000_000
    for batch in pf.iter_batches(batch_size=batch_size, columns=["pre", "post", "syn_count"]):
        total_rows += batch.num_rows
        pre, post, syn = batch.column("pre"), batch.column("post"), batch.column("syn_count")
        mask = pc.and_(pc.is_in(pre, value_set=value_set), pc.is_in(post, value_set=value_set))
        mask = pc.and_(mask, pc.greater_equal(syn, min_syn))
        filtered = batch.filter(mask)
        if filtered.num_rows:
            pres.append(filtered.column("pre").to_numpy(zero_copy_only=False))
            posts.append(filtered.column("post").to_numpy(zero_copy_only=False))
            syns.append(filtered.column("syn_count").to_numpy(zero_copy_only=False))
            kept += filtered.num_rows
        if total_rows >= next_log:
            log(f"  linhas {total_rows}, arestas internas {kept}")
            next_log += 5_000_000
    if not kept:
        raise SystemExit("nenhuma aresta interna encontrada: verifique os ids e o limiar de sinapses")
    return (
        np.concatenate(pres),
        np.concatenate(posts),
        np.concatenate(syns),
    )


def cap_edges(pre: np.ndarray, post: np.ndarray, syn: np.ndarray, max_out: int, max_edges: int):
    """Mantem as sinapses mais fortes respeitando um teto de grau de saida."""
    order = np.argsort(-syn, kind="stable")
    out_degree: Counter[int] = Counter()
    keep = np.zeros(len(order), dtype=bool)
    kept = 0
    for idx in order:
        p = int(pre[idx])
        if out_degree[p] >= max_out:
            continue
        out_degree[p] += 1
        keep[idx] = True
        kept += 1
        if kept >= max_edges:
            break
    return pre[keep], post[keep], syn[keep]


def write_asset(
    out_path: str,
    ids: list[int],
    indices: list[int],
    table: dict,
    pre: np.ndarray,
    post: np.ndarray,
    syn: np.ndarray,
) -> dict:
    index = {root: k for k, root in enumerate(ids)}
    root_to_node = {root: node_i for root, node_i in zip(ids, indices)}
    weights = np.empty(len(syn), dtype=np.float32)
    max_syn = float(syn.max()) if len(syn) else 1.0
    for k in range(len(syn)):
        weights[k] = min(0.35 + 0.75 * math.log1p(float(syn[k])) / math.log1p(max_syn), 1.25)

    label_ids: dict[str, int] = {}
    label_list: list[str] = []
    neuron_rows = []
    role_counts: Counter[str] = Counter()
    nt_counts: Counter[str] = Counter()
    for root, node_i in zip(ids, indices):
        cls = table["cell_class"][node_i]
        ctype = table["cell_type"][node_i]
        sup = table["super_class"][node_i]
        label = cls or ctype or sup or "desconhecido"
        if label not in label_ids:
            label_ids[label] = len(label_list)
            label_list.append(label)
        role = role_of(table, node_i)
        nt = nt_of(table, node_i)
        role_counts[
            {ROLE_INPUT: "input", ROLE_INTER: "inter", ROLE_DOPAMINE: "dopamina", ROLE_OUTPUT: "output"}[role]
        ] += 1
        # Conta o NT efetivamente gravado no asset (com a correção das KCs).
        nt_counts[NT_NAMES[nt]] += 1

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    with open(out_path, "wb") as fh:
        fh.write(MAGIC)
        fh.write(struct.pack(">iiii", VERSION, len(ids), len(pre), len(label_list)))
        for label in label_list:
            raw = label.encode("utf-8")
            fh.write(struct.pack(">H", len(raw)))
            fh.write(raw)
        for root, node_i in zip(ids, indices):
            label = (
                table["cell_class"][node_i]
                or table["cell_type"][node_i]
                or table["super_class"][node_i]
                or "desconhecido"
            )
            role = role_of(table, node_i)
            nt = nt_of(table, node_i)
            fh.write(struct.pack(">qbbH", root, role, nt, label_ids[label]))
        for k in range(len(pre)):
            p = int(index[int(pre[k])])
            pre_root = int(pre[k])
            q = int(index[int(post[k])])
            nt = nt_of(table, root_to_node[pre_root])
            sign = NT_SIGN[nt]
            delay = 2 if int(syn[k]) >= 30 else 1
            fh.write(struct.pack(">iifbb", p, q, float(weights[k]), sign, delay))

    stats = {
        "neurons": len(ids),
        "edges": int(len(pre)),
        "labels": len(label_list),
        "roles": dict(role_counts),
        "neurotransmitters": dict(nt_counts),
        "max_out_degree": int(Counter(int(p) for p in pre).most_common(1)[0][1]) if len(pre) else 0,
        "mean_synapses_per_edge": round(float(syn.mean()), 2) if len(syn) else 0.0,
        "max_synapses": int(syn.max()) if len(syn) else 0,
        "asset_bytes": os.path.getsize(out_path),
    }
    return stats


def main() -> int:
    here = os.path.dirname(os.path.abspath(__file__))
    root = os.path.dirname(here)
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--nodes", default=os.path.join(os.path.expanduser("~"), "Downloads", "nodes.parquet"))
    ap.add_argument("--connections", default=os.path.join(os.path.expanduser("~"), "Downloads", "connections.parquet"))
    ap.add_argument("--out", default=os.path.join(root, "app", "src", "main", "assets", "connectome", "flywire_mb.bin"))
    ap.add_argument("--stats", default=os.path.join(root, "tools", "connectome_stats.json"))
    ap.add_argument("--min-syn", type=int, default=5, help="limiar de sinapses por aresta (FAFB usa 5)")
    ap.add_argument("--max-out", type=int, default=90, help="teto de grau de saida por neuronio")
    ap.add_argument("--max-edges", type=int, default=120000, help="teto de arestas no asset")
    ap.add_argument("--seed", type=int, default=42)
    args = ap.parse_args()

    for path, name in ((args.nodes, "nodes.parquet"), (args.connections, "connections.parquet")):
        if not os.path.exists(path):
            raise SystemExit(f"arquivo nao encontrado: {name} em {path}")

    table = load_nodes(args.nodes)
    log("selecionando neuronios do subcircuito de aprendizado")
    indices = select_neurons(table, args.seed)
    ids = [int(table["root_id"][i]) for i in indices]
    log(f"selecionados {len(ids)} neuronios")

    log("varrendo connections.parquet (15M de linhas)")
    pre, post, syn = collect_edges(args.connections, set(ids), args.min_syn)
    log(f"arestas internas com >= {args.min_syn} sinapses: {len(pre)}")

    pre, post, syn = cap_edges(pre, post, syn, args.max_out, args.max_edges)
    log(f"arestas mantidas apos teto de grau/volume: {len(pre)}")

    stats = write_asset(args.out, ids, indices, table, pre, post, syn)
    with open(args.stats, "w", encoding="utf-8") as fh:
        json.dump(stats, fh, indent=2, ensure_ascii=False)
    log("asset gravado: " + args.out)
    log(json.dumps(stats, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
