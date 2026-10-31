# Mini Conectoma-LLM

Motor matematico em miniatura, inspirado no app **Conectoma** (repositorio
`joaohenriqueribeironunes-ops/conectoma`, v1.6) e no seu nucleo original.

## O que e

Nao e um LLM completo. E uma **memoria associativa** que aprende FATOS a
partir dos dados que voce fornece e conversa recuperando-os, como a mosca do
Conectoma:

- **Fatos**: linhas `pergunta = resposta` (ou texto livre, que vira fatos).
- **Espaco semantico**: vetores de co-ocorrencia (janela de 3 palavras +
  co-ocorrencia de documento), cosseno entre palavras. Nada e pre-treinado.
- **Recuperacao**: score = 0.40*lexical + 0.22*semantico, com limiar que cai
  com a experiencia (plasticidade).
- **Composicao**: costura a resposta a partir do fato mais proximo + fatos de
  apoio, com conectivos variados. Nada e inventado.
- **Conexoes divergentes**: rede de conceitos; uma pergunta puxa ideias
  vizinhas (CONECTA COM).
- **Plasticidade**: quanto mais aprende, mais facil fica aprender (limiar cai).
- **Curiosidade**: escolhe o fato mais "esquecido" para falar espontaneamente.
- **Dopamina**: recompensa interna por novidade.

## Como usar

Treinar:
```
python train.py dados_exemplo.txt modelo.pkl
```

Conversar:
```
python chat.py --model modelo.pkl
```

Treinar ao iniciar o chat:
```
python chat.py --data meus_dados.txt --model modelo.pkl
```

## Comandos no chat

- `/treinar <texto>` ensina um texto.
- `/arquivo <path>` treina com um arquivo.
- `/salvar` salva o modelo.
- `/estado` mostra estado interno.
- `/espontaneo` gera fala espontanea.
- linha vazia gera fala espontanea.
- `/sair` encerra.

Formato de dados: `pergunta = resposta` (tambem aceita `|` e `=>`).
Linhas `#` ou `//` sao ignoradas.

## Notas

- Tudo em Python puro (stdlib), sem dependencias externas.
- Persistencia em pickle (`.pkl`).
- Quanto mais dados e conversa, mais ele aprende (plasticidade sobe).
