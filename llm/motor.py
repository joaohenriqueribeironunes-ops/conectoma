# -*- coding: utf-8 -*-
"""
Mini motor matematico de memoria associativa, inspirado no app "Conectoma"
(GitHub: joaohenriqueribeironunes-ops/conectoma) e no nucleo Rust do mesmo
repositorio.

Em vez de gerar palavras aleatorias, este motor aprende FATOS (pergunta=resposta)
a partir dos dados que voce fornece, constrói um ESPACO SEMANTICO (vetores de
co-ocorrencia) sobre o proprio vocabulario, RECUPERA a memoria mais proxima da
pergunta e COSTURA a resposta a partir dos fatos ensinados (composicao), com
CONEXOES DIVERGENTES (rede de conceitos) e PLASTICIDADE (quanto mais aprende,
mais facil fica aprender). Tudo em Python puro (stdlib).
"""

import re
import math
import random
import pickle
import os


# --------------------------------------------------------------------------- #
# TextCode: normalizacao, tokens, similaridade lexical (portado do Kotlin)
# --------------------------------------------------------------------------- #
# Stopwords desativadas para manter consistencia: perguntas curtas como
# "como voce esta" nao podem virar vazias. (O app original remove, mas aqui
# preferimos casamento exato de perguntas.)
STOPWORDS = set()


def normalize(text):
    # remove acentos
    import unicodedata
    decomposed = unicodedata.normalize("NFD", text.lower())
    t = "".join(c for c in decomposed if unicodedata.category(c) != "Mn")
    t = re.sub(r"[^a-z0-9 ]", " ", t)
    t = re.sub(r"\s+", " ", t).strip()
    return t


def stem(word):
    if len(word) > 4 and word.endswith("s"):
        return word[:-1]
    return word


def tokens(text):
    toks = normalize(text).split()
    out = []
    for t in toks:
        if len(t) >= 2 and t not in STOPWORDS:
            out.append(stem(t))
    if not out:
        # fallback: nao descartar tudo, manter palavras de pergunta
        for t in toks:
            if len(t) >= 2:
                out.append(stem(t))
    return out


def terms(text):
    base = tokens(text)
    out = set(base)
    for i in range(len(base) - 1):
        out.add(base[i] + "_" + base[i + 1])
    return out


def trigrams(text):
    clean = normalize(text).replace(" ", "")
    if len(clean) < 3:
        return set([clean]) if clean else set()
    out = set()
    for i in range(len(clean) - 2):
        out.add(clean[i:i + 3])
    return out


def lexical_similarity(query, reference):
    q = terms(query)
    r = terms(reference)
    if not q or not r:
        return 0.0
    common = len(q & r)
    union = len(q | r)
    jaccard = common / union if union else 0.0
    coverage = common / len(q) if q else 0.0
    qtri = trigrams(query)
    rtri = trigrams(reference)
    tri_common = len(qtri & rtri)
    tri_union = len(qtri | rtri)
    tri_jaccard = tri_common / tri_union if tri_union else 0.0
    score = 0.42 * jaccard + 0.38 * coverage + 0.20 * tri_jaccard
    return max(0.0, min(1.0, score))


def parse_facts(raw):
    out = []
    for line in raw.splitlines():
        trimmed = line.strip()
        if not trimmed or trimmed.startswith("#") or trimmed.startswith("//"):
            continue
        for sep in ("=>", "|", "="):
            idx = trimmed.find(sep)
            if idx > 0:
                q = trimmed[:idx].strip()
                a = trimmed[idx + len(sep):].strip()
                if q and a:
                    out.append((q, a))
                    break
    return out


# --------------------------------------------------------------------------- #
# SemanticSpace: vetores de co-ocorrencia (portado do Kotlin)
# --------------------------------------------------------------------------- #
class SemanticSpace:
    WINDOW = 3
    MIN_EXPANSION = 0.18
    DOCUMENT_LIMIT = 60
    DOCUMENT_WEIGHT = 0.35

    def __init__(self, contexts):
        self.contexts = contexts  # dict term -> dict other -> float

    @property
    def vocabulary_size(self):
        return len(self.contexts)

    def knows(self, term):
        return term in self.contexts

    def neighbours(self, term, limit=4):
        vec = self.contexts.get(term)
        if not vec:
            return []
        out = []
        for cand, other in self.contexts.items():
            if cand == term:
                continue
            sc = self._cosine(vec, other)
            if sc > 0:
                out.append((cand, sc))
        out.sort(key=lambda x: -x[1])
        return out[:limit]

    def similarity(self, a, b):
        va = self.contexts.get(a)
        vb = self.contexts.get(b)
        if not va or not vb:
            return 0.0
        return self._cosine(va, vb)

    def analyse(self, query, limit=6):
        terms_list = tokens(query)
        # distinct preserving order
        seen = set()
        uniq = []
        for t in terms_list:
            if t not in seen:
                seen.add(t)
                uniq.append(t)
        near = {}
        for t in uniq:
            near[t] = self.neighbours(t, 6)
        own = set(uniq)
        best = {}
        for t in uniq:
            for cand, sc in near.get(t, []):
                if cand in own or sc < self.MIN_EXPANSION:
                    continue
                if cand not in best or sc > best[cand]:
                    best[cand] = sc
        expanded = [k for k, _ in sorted(best.items(), key=lambda x: -x[1])[:limit]]
        return {"terms": uniq, "near": near, "expanded": expanded}

    def overlap(self, query_terms_or_analysis, reference):
        # aceita analysis dict ou lista de termos; aqui esperamos analysis dict
        if isinstance(query_terms_or_analysis, dict):
            terms = query_terms_or_analysis.get("terms", [])
            near = query_terms_or_analysis.get("near", {})
        else:
            terms = query_terms_or_analysis
            near = {}
        if not terms:
            return 0.0
        ref = set(tokens(reference))  # tokens ja stemmizados? reference texto
        # reference deve ser tokenizado fora; mas tokens() ok
        if not ref:
            return 0.0
        s = 0.0
        for term in terms:
            if term in ref:
                s += 1.0
                continue
            via = None
            for cand, sc in near.get(term, []):
                if cand in ref:
                    via = (cand, sc)
                    break
            if via is not None:
                s += 0.8 * via[1]
        return max(0.0, min(1.0, s / len(terms)))

    @staticmethod
    def build(facts):
        docs = []
        for f in facts:
            docs.append(tokens(f["topic"] + " " + f["question"] + " " + f["answer"]))
        return SemanticSpace.from_documents(docs)

    @staticmethod
    def from_documents(documents):
        contexts = {}
        for doc in documents:
            if len(doc) < 2:
                if doc:
                    contexts.setdefault(doc[0], {})
                continue
            # contexto local
            for i in range(len(doc)):
                word = doc[i]
                bucket = contexts.setdefault(word, {})
                frm = max(0, i - SemanticSpace.WINDOW)
                to = min(len(doc) - 1, i + SemanticSpace.WINDOW)
                for j in range(frm, to + 1):
                    if j == i:
                        continue
                    other = doc[j]
                    bucket[other] = bucket.get(other, 0.0) + 1.0 / abs(i - j)
            # co-ocorrencia de documento
            if len(doc) <= SemanticSpace.DOCUMENT_LIMIT:
                uniq = list(dict.fromkeys(doc))
                for i in range(len(uniq)):
                    for j in range(i + 1, len(uniq)):
                        a = uniq[i]
                        b = uniq[j]
                        contexts.setdefault(a, {})
                        contexts.setdefault(b, {})
                        contexts[a][b] = contexts[a].get(b, 0.0) + SemanticSpace.DOCUMENT_WEIGHT
                        contexts[b][a] = contexts[b].get(a, 0.0) + SemanticSpace.DOCUMENT_WEIGHT
        return SemanticSpace(contexts)

    def _cosine(self, a, b):
        small = a if len(a) <= len(b) else b
        large = b if len(a) <= len(b) else a
        dot = 0.0
        na = 0.0
        nb = 0.0
        for v in a.values():
            na += v * v
        for v in b.values():
            nb += v * v
        for key, val in small.items():
            o = large.get(key)
            if o is not None:
                dot += val * o
        if na <= 1e-9 or nb <= 1e-9:
            return 0.0
        return max(0.0, min(1.0, dot / (math.sqrt(na) * math.sqrt(nb))))


# --------------------------------------------------------------------------- #
# Concepts: rede de conceitos e conexoes divergentes
# --------------------------------------------------------------------------- #
def extract_concepts(text):
    return [t for t in tokens(text) if len(t) >= 3][:30]


def shared_concepts(a, b):
    sa = set(a)
    return [x for x in b if x in sa]


def divergent_ideas(query, facts, exclude_id=None, limit=3):
    if len(facts) < 2:
        return []
    concepts = {}
    for f in facts:
        concepts[f["id"]] = extract_concepts(f["topic"] + " " + f["question"] + " " + f["answer"])
    query_concepts = extract_concepts(query)
    seed = {}
    for f in facts:
        lex = lexical_similarity(query, f["topic"] + " " + f["question"])
        if query_concepts:
            ov = len(shared_concepts(query_concepts, concepts[f["id"]])) / len(query_concepts)
        else:
            ov = 0.0
        seed[f["id"]] = 0.7 * lex + 0.3 * ov

    activation = dict(seed)
    for _ in range(2):
        nxt = {}
        for i in range(len(facts)):
            for j in range(i + 1, len(facts)):
                a = facts[i]
                b = facts[j]
                common = shared_concepts(concepts[a["id"]], concepts[b["id"]])
                if not common:
                    continue
                union = len(set(concepts[a["id"]]) | set(concepts[b["id"]]))
                if union == 0:
                    continue
                weight = len(common) / union
                act_a = activation.get(a["id"], 0.0)
                act_b = activation.get(b["id"], 0.0)
                nxt[a["id"]] = nxt.get(a["id"], 0.0) + 0.6 * weight * act_b
                nxt[b["id"]] = nxt.get(b["id"], 0.0) + 0.6 * weight * act_a
        for fid, val in nxt.items():
            activation[fid] = activation.get(fid, 0.0) + val

    results = []
    for f in facts:
        if exclude_id is not None and f["id"] == exclude_id:
            continue
        score = activation.get(f["id"], 0.0)
        if score < 0.07:
            continue
        # via
        own = concepts[f["id"]]
        direct = shared_concepts(query_concepts, own)
        if direct:
            via = ", ".join(direct[:3])
        else:
            via = "rede de conceitos"
        results.append({"fact": f, "score": max(0.0, min(1.0, score)), "via": via})
    results.sort(key=lambda x: -x["score"])
    return results[:limit]


# --------------------------------------------------------------------------- #
# MiniLLM: memoria associativa
# --------------------------------------------------------------------------- #
class MiniLLM:
    def __init__(self, seed=42):
        self.rng = random.Random(seed)
        self.facts = []          # list of dict
        self.next_id = 1
        self.semantic = None     # SemanticSpace
        self.plasticity_exp = 0.0  # experiencia acumulada
        self.dopamine = 0.15
        self.tick = 0
        self.history = []
        self.last_used = {}      # fact_id -> tick
        self.auto_stimuli = 0
        # config
        self.floor = 0.16
        self.support_ratio = 0.55
        self.max_supports = 2

    # ------------------------------------------------------------------ #
    # aprendizado
    # ------------------------------------------------------------------ #
    def _add_fact(self, topic, question, answer):
        f = {
            "id": self.next_id,
            "topic": topic.strip() if topic.strip() else "geral",
            "question": question.strip(),
            "answer": answer.strip(),
            "strength": 1.0,
            "uses": 0,
            "concepts": extract_concepts((topic + " " + question + " " + answer)),
        }
        self.next_id += 1
        self.facts.append(f)
        return f

    def train_text(self, text):
        # tenta parsear fatos explicitos
        parsed = parse_facts(text)
        explicit = True
        if not parsed:
            explicit = False
            sents = re.split(r"(?<=[.!?])\s+|\n+", text.strip())
            sents = [s.strip() for s in sents if s.strip()]
            if not sents:
                return {"facts": 0}
            parsed = [(s, s) for s in sents]
        created = 0
        for q, a in parsed:
            existing = None
            for f in self.facts:
                if normalize(f["question"]) == normalize(q):
                    existing = f
                    break
            if existing is not None:
                existing["strength"] = min(2.0, existing.get("strength", 1.0) + 0.2)
                # so sobrescreve a resposta se vier de fato explicito e for mais informativa
                if explicit and len(a.strip()) >= len(existing.get("answer", "")):
                    existing["answer"] = a.strip() or existing["answer"]
                continue
            toks = tokens(q)
            topic = " ".join(toks[:3]) if toks else "geral"
            self._add_fact(topic, q, a)
            created += 1
        # rebuild semantic space
        self.semantic = SemanticSpace.build(self.facts)
        # plasticidade
        self.plasticity_exp += created * 1.0
        ease = 1.0 - math.exp(-self.plasticity_exp / 20.0)
        # dopamina por novidade
        novelty = created * 0.05
        self.dopamine = (self.dopamine + novelty) * 0.992 - 0.0005
        self.dopamine = max(0.05, min(1.5, self.dopamine))
        self.tick += 1
        return {"facts": created, "total_facts": len(self.facts),
                "vocab": self.semantic.vocabulary_size if self.semantic else 0,
                "plasticidade": round(ease * 100, 1),
                "dopamina": round(self.dopamine * 100, 1)}

    def train_corpus(self, text, verbose=True):
        # train_text ja trata texto inteiro
        res = self.train_text(text)
        if verbose:
            print("Treino:", res)
        return res

    # ------------------------------------------------------------------ #
    # recuperacao
    # ------------------------------------------------------------------ #
    def _ease(self):
        return 1.0 - math.exp(-self.plasticity_exp / 20.0)

    def _threshold(self):
        ease = self._ease()
        return max(0.30, 0.42 - 0.10 * ease)

    def _score_fact(self, query, fact, analysis):
        # lexical
        lex = lexical_similarity(query, fact["topic"] + " " + fact["question"] + " " + fact["answer"])
        # semantico: overlap entre conceitos da pergunta e do fato
        if analysis is not None:
            sem = self.semantic.overlap(analysis, fact["topic"] + " " + fact["question"] + " " + fact["answer"]) if self.semantic else 0.0
        else:
            sem = 0.0
        # engrama nao implementado (0)
        engram = 0.0
        raw = 0.40 * lex + 0.22 * sem + 0.38 * engram
        strength_factor = 0.8 + 0.2 * fact.get("strength", 1.0)
        score = raw * strength_factor
        return score, lex, sem

    def retrieve(self, query):
        if not self.facts:
            return []
        analysis = self.semantic.analyse(query) if self.semantic else None
        ranked = []
        for f in self.facts:
            sc, lex, sem = self._score_fact(query, f, analysis)
            ranked.append({"fact": f, "score": sc, "lexical": lex, "semantic": sem})
        ranked.sort(key=lambda x: -x["score"])
        # filtrar por threshold
        thr = self._threshold()
        # aceita se score >= thr ou lexical >= 0.55
        out = []
        for r in ranked:
            if r["score"] >= thr or r["lexical"] >= 0.55:
                out.append(r)
        # se nenhum passou, retorna os top mesmo (para composicao decidir)
        if not out and ranked:
            out = ranked[:1]
        return out

    def compose(self, query, ranked):
        if not ranked:
            return None
        top = ranked[0]
        if top["score"] < self.floor:
            return None
        top_answer = top["fact"]["answer"].strip()
        supports = []
        for r in ranked[1:]:
            if r["score"] >= top["score"] * self.support_ratio and r["fact"]["answer"].strip() != top_answer:
                if r["fact"]["answer"].strip().lower() not in [s["fact"]["answer"].strip().lower() for s in supports]:
                    supports.append(r)
            if len(supports) >= self.max_supports:
                break
        used = [top] + supports
        bridges = [
            "Também guardei algo ligado a isso: ",
            "Isso se conecta com outro fato que aprendi: ",
            "Na mesma rede de ideias, tenho também: ",
            "Aproximei outra memória a isso: ",
        ]
        bridge = bridges[abs(hash(query)) % len(bridges)]
        text = top_answer[0].upper() + top_answer[1:] if top_answer else ""
        if text and text[-1] not in ".!?":
            text += "."
        for idx, r in enumerate(supports):
            ans = r["fact"]["answer"].strip()
            if ans:
                ans = ans[0].upper() + ans[1:]
                if ans and ans[-1] not in ".!?":
                    ans += "."
            conn = bridge if idx == 0 else "E ainda: "
            text += " " + conn + ans
        text = re.sub(r"\s+", " ", text).strip()
        # linked (expanded terms that appear in vocabulary)
        analysis = self.semantic.analyse(query) if self.semantic else None
        expanded = analysis["expanded"] if analysis else []
        vocab = set()
        for r in used:
            vocab |= set(tokens(r["fact"]["topic"] + " " + r["fact"]["question"] + " " + r["fact"]["answer"]))
        linked = [t for t in expanded if t in vocab][:4]
        confidence = top["score"]
        return {"text": text, "facts": [r["fact"] for r in used],
                "sources": list(dict.fromkeys([r["fact"]["topic"] for r in used])),
                "linked": linked, "confidence": confidence}

    # ------------------------------------------------------------------ #
    # conversa
    # ------------------------------------------------------------------ #
    def respond(self, user_text):
        # aprender com o que o usuario disse (como fato)
        self.train_text(user_text)
        ranked = self.retrieve(user_text)
        comp = self.compose(user_text, ranked)
        if comp is None:
            # aprender a pergunta como fato de "nao sei"? apenas responder
            resp = "Ainda não aprendi sobre isso. Me ensine com 'pergunta = resposta'."
        else:
            resp = comp["text"]
            # reconsolidacao: fortalece fatos usados
            for f in comp["facts"]:
                f["strength"] = min(2.0, f.get("strength", 1.0) + 0.1)
                f["uses"] = f.get("uses", 0) + 1
                self.last_used[f["id"]] = self.tick
        # divergentes
        divs = []
        if self.facts:
            top_id = comp["facts"][0]["id"] if comp else None
            divs = divergent_ideas(user_text, self.facts, exclude_id=top_id, limit=3)
        self.history.append((user_text, resp))
        self.tick += 1
        # dopamina leve
        self.dopamine = (self.dopamine + 0.02) * 0.992 - 0.0005
        self.dopamine = max(0.05, min(1.5, self.dopamine))
        return resp

    def spontaneous(self):
        # curiosidade: escolher fato mais esquecido
        if not self.facts:
            return "Ainda não aprendi nada. Me ensine algo!"
        # pick forgotten
        best = None
        best_score = -1
        for f in self.facts:
            esquecido = self.tick - self.last_used.get(f["id"], 0)
            score = esquecido * (1.0 + 0.5 * self._ease()) * (0.6 + 0.8 * self.rng.random())
            if score > best_score:
                best_score = score
                best = f
        if best is None:
            best = self.rng.choice(self.facts)
        self.last_used[best["id"]] = self.tick
        # montar fala a partir do fato
        ans = best["answer"].strip()
        if ans:
            ans = ans[0].upper() + ans[1:]
            if ans[-1] not in ".!?":
                ans += "."
        # talvez adicionar divergente
        divs = divergent_ideas(best["question"], self.facts, exclude_id=best["id"], limit=1)
        if divs:
            d = divs[0]["fact"]
            a2 = d["answer"].strip()
            if a2:
                a2 = a2[0].upper() + a2[1:]
                if a2[-1] not in ".!?":
                    a2 += "."
                ans += " " + "Isso me lembra também: " + a2
        self.history.append(("", ans))
        self.tick += 1
        self.auto_stimuli = getattr(self, "auto_stimuli", 0) + 1
        return ans

    # ------------------------------------------------------------------ #
    # persistencia
    # ------------------------------------------------------------------ #
    def save(self, path):
        with open(path, "wb") as f:
            pickle.dump(self, f)

    @staticmethod
    def load(path):
        with open(path, "rb") as f:
            return pickle.load(f)

    # ------------------------------------------------------------------ #
    # estado
    # ------------------------------------------------------------------ #
    def state(self):
        return {
            "facts": len(self.facts),
            "vocab": self.semantic.vocabulary_size if self.semantic else 0,
            "plasticidade": round(self._ease() * 100, 1),
            "experiencia": round(self.plasticity_exp, 1),
            "dopamina": round(self.dopamine * 100, 1),
            "threshold": round(self._threshold(), 3),
            "tick": self.tick,
        }
