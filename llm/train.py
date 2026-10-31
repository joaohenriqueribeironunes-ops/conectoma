# -*- coding: utf-8 -*-
"""
Treina o mini motor com um ou mais arquivos de texto e salva o modelo.

Uso:
  python train.py dados.txt modelo.pkl
  python train.py dados1.txt dados2.txt modelo.pkl
"""

import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from motor import MiniLLM


def main():
    if len(sys.argv) < 3:
        print("Uso: python train.py <arquivo(s)> <modelo.pkl>")
        sys.exit(1)
    *files, out = sys.argv[1:]
    m = MiniLLM()
    for f in files:
        if not os.path.exists(f):
            print("Arquivo nao encontrado:", f)
            continue
        print("Treinando com", f)
        with open(f, "r", encoding="utf-8") as fh:
            txt = fh.read()
        m.train_corpus(txt)
    m.save(out)
    print("Modelo salvo em", out)
    print(m.state())


if __name__ == "__main__":
    main()
