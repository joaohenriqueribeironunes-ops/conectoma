# -*- coding: utf-8 -*-
"""
Chat interativo com o mini motor matematico (inspirado no conectoma).

Uso:
  python chat.py [--model modelo.pkl] [--data dados.txt] [--seed 42]

Comandos durante o chat:
  /treinar <texto>   -> ensina um texto ao motor
  /arquivo <path>    -> treina com um arquivo
  /salvar            -> salva o modelo
  /estado            -> mostra estado interno
  /espontaneo        -> gera uma fala espontanea (curiosidade)
  /sair              -> encerra
  (linha vazia)      -> gera fala espontanea
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from motor import MiniLLM

PY = sys.executable


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default="modelo.pkl")
    ap.add_argument("--data", default=None, help="arquivo de texto para treinar ao iniciar")
    ap.add_argument("--seed", type=int, default=42)
    args = ap.parse_args()

    if os.path.exists(args.model):
        print("Carregando modelo:", args.model)
        m = MiniLLM.load(args.model)
    else:
        print("Criando modelo novo (seed=%d)" % args.seed)
        m = MiniLLM(seed=args.seed)

    if args.data and os.path.exists(args.data):
        print("Treinando com", args.data)
        with open(args.data, "r", encoding="utf-8") as f:
            txt = f.read()
        m.train_corpus(txt)
        m.save(args.model)
        print("Modelo salvo em", args.model)

    print("\n=== Mini Conectoma-LLM ===")
    print("Digite algo para conversar. /sair para encerrar. Linha vazia = fala espontanea.\n")

    while True:
        try:
            line = input("voce> ")
        except (EOFError, KeyboardInterrupt):
            print()
            break
        line = line.strip()
        if not line:
            resp = m.spontaneous()
            print("motor>", resp)
            print("  ", m.state())
            continue
        if line.lower() in ("/sair", "sair", "exit", "quit"):
            break
        if line.startswith("/treinar "):
            txt = line[len("/treinar "):]
            m.train_text(txt)
            print("  aprendido. estado:", m.state())
            continue
        if line.startswith("/arquivo "):
            path = line[len("/arquivo "):].strip()
            if os.path.exists(path):
                with open(path, "r", encoding="utf-8") as f:
                    txt = f.read()
                m.train_corpus(txt)
                print("  treinado com", path, m.state())
            else:
                print("  arquivo nao encontrado:", path)
            continue
        if line == "/salvar":
            m.save(args.model)
            print("  salvo em", args.model)
            continue
        if line == "/estado":
            print("  ", m.state())
            continue
        if line == "/espontaneo":
            resp = m.spontaneous()
            print("motor>", resp)
            print("  ", m.state())
            continue

        resp = m.respond(line)
        print("motor>", resp)
        print("   ", m.state())

    m.save(args.model)
    print("Modelo salvo em", args.model)
    print("Ate mais.")


if __name__ == "__main__":
    main()
