# Conectoma 1.5

Aplicativo Android nativo que usa um recorte real do conectoma da mosca
(*Drosophila melanogaster*, FlyWire FAFB v783) como circuito de aprendizagem.
Você ensina fatos em texto, o circuito recebe estimulação sensorial, dispara,
recebe pulsos de dopamina computadorizada e tem os pesos sinápticos alterados
por STDP de três fatores. As memórias ficam numa base SQLite local e podem ser
consultadas por pergunta.

O app se chama **Conectoma** desde a versão 1.3 (antes era FlyMind). O APK
atual está na release
[v1.5.0](https://github.com/joaohenriqueribeironunes-ops/conectoma/releases/tag/v1.5.0)
(`Conectoma-1.5.apk`, 20 MB, `versionCode 6`, assinado com a chave de debug do
Android). SHA-256:
`8d806b5d0c9448177a04d8447e8f00a277697ff0ca4b8e64046590c2a8575903`. Os `.apk`
não são versionados — o `.gitignore` os exclui — só ficam na release.

## O que o app faz

| Aba | Função |
|---|---|
| **Ensinar** | Ensina um fato (tema, pergunta, resposta), um caderno inteiro ou uma **página da web pela URL**, com 3 a 12 ensaios por trecho. Mostra disparos, dopamina, estabilidade do engrama e mudança de peso. |
| **Conversar** | Pergunta em texto. A mosca responde só com fatos que aprendeu; se não souber, diz que não aprendeu. Exibe confiança, similaridade do engrama e as **conexões divergentes** que a pergunta despertou. |
| **Memória** | Lista as memórias, permite reconsolidar (treinar de novo), apagar, limpar tudo e exportar JSON. |
| **Voz** | Escolhe qualquer voz TTS instalada no aparelho ou importa um **arquivo de voz pronto** (gerado por IA), ajusta tom e velocidade, liga a fala automática e testa. |
| **Cérebro** | Estatísticas do conectoma, raster de disparos, plasticidade acumulada, estimulação espontânea e restauração dos pesos originais. |
| **Sistema** | Aparelho/Android/HyperOS detectados, atalhos para bateria, autostart e restrições da MIUI/HyperOS, e opção de manter a tela ligada. |

A interface segue a linguagem visual do iOS, não o Material:

- **Tipografia** em [Inter](https://rsms.me/inter/) (licença OFL, embarcada no
  APK em `res/font`), a substituta livre mais próxima do SF Pro: large title de
  34 dp, headline de 17 dp, footnote de 13 dp, com tracking negativo.
- **Cor** de sistema: azul `#007AFF`, laranja `#FF9500`, verde `#34C759`,
  vermelho `#FF3B30`, fundo de listas agrupadas `#F2F2F7` e separador de 0,5 dp.
- **Chrome**: cabeçalho de título grande com botão circular cinza, tab bar de
  49 dp com ícones de 25 dp e rótulos de 10 dp, sem pílula de seleção, e barra
  de navegação compacta com botão “Voltar” em pílula nas telas internas.
- **Listas agrupadas**: cartões brancos de 10 dp sem borda, cabeçalho de seção
  em caixa-alta de 13 dp e rodapé explicativo, como os Ajustes.
- **Controles**: botões cheios de 50 dp com cantos de 12 dp, botões tonalizado e
  cinza, switch verde, slider de trilho fino, barra de progresso de 4 dp e
  alertas centralizados com ações azuis.
- **Aparência clara e escura**: a paleta de sistema tem as duas variantes
  (`systemBlue #007AFF` / `#0A84FF`, fundo `#F2F2F7` / `#000000`, cartões
  `#FFFFFF` / `#1C1C1E`), escolhidas por `isSystemInDarkTheme()`. As janelas
  usam `values-night` (fundo preto e ícones claros nas barras de sistema), então
  o app acompanha o tema do HyperOS sem precisar de ajuste interno.
- **Sem ripple**: o indicador radial do Android foi desligado; botões e linhas
  respondem escurecendo, como no iOS.

O ícone é a arte da rede de neurônios em gradiente, distribuída como ícone
adaptativo: uma camada de fundo de 108 dp inteira (a rede fica na zona segura,
então nenhuma máscara de launcher a corta) e um ícone monocromático para os
ícones temáticos. [tools/make_app_icon.py](tools/make_app_icon.py) regenera as
camadas a partir da arte original.

## Instalação no Redmi Note 11S (HyperOS 1)

Requisitos: Android 8.0 (API 26) ou superior. O APK foi compilado com
`minSdk 26`, `targetSdk 35` e `compileSdk 36`.

1. Baixe
   [Conectoma-1.5.apk](https://github.com/joaohenriqueribeironunes-ops/conectoma/releases/download/v1.5.0/Conectoma-1.5.apk)
   e copie para o celular (cabo USB, Google Drive, Telegram etc.).
2. Abra o arquivo no gerenciador de arquivos e permita "instalar apps
   desconhecidos" para o app usado na instalação.
3. Se o Play Protect avisar que o app não é reconhecido, escolha instalar
   mesmo assim. O APK é assinado com a chave de debug, não com uma chave de
   loja.
4. Alternativa por USB, com depuração USB ativada:

```bash
adb install -r Conectoma-1.5.apk
```

Para atualizar depois, o `versionCode` precisa ser maior ou o app precisa ser
desinstalado antes.

### Primeiros passos

1. Abra **Ensinar**. Preencha tema, pergunta e resposta. Exemplo:
   - Tema: `Biologia`
   - Pergunta: `O que é fotossíntese?`
   - Resposta: `É o processo em que plantas usam luz, água e CO2 para produzir glicose e oxigênio.`
2. Toque em **Ensinar à mosca** e acompanhe os ensaios. Cada ensaio roda
   160 ms de estimulação + 120 ms de recompensa, com 3 pulsos de dopamina.
3. Vá para **Conversar** e pergunte com as mesmas palavras-chave. Exemplo:
   `Como funciona a fotossíntese?`
4. Confira a memória em **Memória** e a plasticidade em **Cérebro**.

### Caderno em lote

Na aba **Ensinar**, escolha **Caderno**. Cada linha é uma memória:

```text
O que é fotossíntese? = Processo que converte luz em energia química.
Quem foi Santos Dumont? = Pioneiro da aviação; voou com o 14-Bis em 1906.
O que é entropia? = Medida da desordem de um sistema.
```

Separadores aceitos: `=`, `|` e `=>`. Linhas iniciadas por `#` ou `//` são
ignoradas. Um caderno grande roda um fato por vez; o app mostra o progresso e
pode manter a tela ligada durante a sessão.

### Página inteira por URL

Na aba **Ensinar**, o painel **Aprender de um site** recebe o endereço de uma
página (com ou sem `https://`) e a transforma em memórias:

1. **Buscar página** baixa só aquela URL (timeout de 15 s, teto de 2,5 MB) e
   mostra título, host, número de trechos e quantidade de caracteres.
2. **Aprender site** treina trecho por trecho, com os mesmos ensaios,
   dopamina e consolidação do caderno em lote.

Regras da extração ([WebSource.kt](app/src/main/java/com/flymind/brain/WebSource.kt)):

- `script`, `style`, `noscript`, `svg`, `iframe`, formulários e comentários são
  descartados; cada título (`h1`–`h6`) abre um trecho novo, e o texto até o
  próximo título vira o corpo daquele trecho.
- Parágrafos com menos de 60 caracteres (menu, aviso de cookies) são ignorados;
  ficam no máximo 12 trechos de 700 caracteres cada.
- Cada trecho vira um fato: o tema é o campo **Tema** (ou o título da página),
  a pergunta é o título do trecho e a resposta é o texto do trecho.
- Os links da página **não** são seguidos: o app não percorre o site inteiro
  nem faz rastreamento.
- Caracteres especiais e entidades HTML são decodificados; a codificação é
  lida do cabeçalho `Content-Type` e, se faltar, do próprio HTML.

### Voz

Existem duas fontes de fala, escolhidas na aba **Voz**:

**1. Voz do sistema (TTS)**

O app usa o motor TTS do Android, sem nuvem:

1. No HyperOS, instale/atualize o Google TTS em
   **Configurações > Idiomas e entrada > Saída de texto para fala**.
2. Baixe as vozes desejadas (inclusive vozes neurais, se disponíveis).
3. No Conectoma, aba **Voz**, escolha a voz, ajuste tom/velocidade e use
   **Ouvir** para testar. A fala automática das respostas pode ser desligada.

**2. Seu arquivo de voz (gerado por IA)**

1. Gere ou baixe um arquivo de voz: MP3, M4A/AAC, WAV, OGG, FLAC, AMR ou 3GP.
2. Na aba **Voz**, toque em **Escolher arquivo de voz** e selecione o arquivo
   (o seletor do Android não exige nenhuma permissão).
3. O arquivo é copiado para o armazenamento interno do app — nome, tamanho e
   duração aparecem no painel, com **Ouvir** e **Remover**.
4. Ligue **Usar meu arquivo nas respostas** e a mosca toca esse arquivo quando
   responder, no lugar do TTS. O texto continua aparecendo na conversa.

O arquivo é tocado inteiro como a fala da mosca: o app não sintetiza texto novo
a partir dele, o que exigiria um modelo de clonagem de voz embarcado.

### HyperOS

A integração é feita no que o Android permite a um app comum, sem root:

- tema claro e escuro no estilo iOS, seguindo o tema do sistema, ícone
  monocromático para os ícones temáticos do HyperOS e barras de sistema
  transparentes com ícones que se ajustam ao fundo;
- tela cheia edge-to-edge;
- atalhos que abrem as telas nativas de bateria, autostart e restrições de
  segundo plano da MIUI/HyperOS;
- nenhum serviço em segundo plano e nenhuma permissão sensível; a internet só
  é usada para baixar a página que você cola na aba Ensinar;
- opção de manter a tela ligada durante cadernos grandes.

Se o HyperOS matar o app ao apagarem a tela, libere o Conectoma em
**Autostart**, **Economia de bateria** e **Restrições de segundo plano**. Os
botões da aba **Sistema** abrem essas telas.

## Como o circuito funciona

### Dados reais

O asset [flywire_mb.bin](app/src/main/assets/connectome/flywire_mb.bin)
(716 KB) é um subcircuito extraído de:

- **FlyWire FAFB v783**: 139.255 neurônios e 15.091.983 pares direcionados
  (DOI [10.5281/zenodo.10676866](https://doi.org/10.5281/zenodo.10676866)).
- **Anotações de Schlegel et al. 2024** (DOI
  [10.1038/s41586-024-07686-5](https://doi.org/10.1038/s41586-024-07686-5)).

O recorte embarcado tem:

| Item | Valor |
|---|---:|
| Neurônios | 6.909 |
| Sinapses (arestas ≥ 5 sinapses) | 44.893 |
| Células de Kenyon | 3.000 |
| Neurônios dopaminérgicos (DAN) | 331 |
| Entradas sensoriais | 1.016 |
| Interneurônios | 5.076 |
| Saídas (MBON, descendentes, motor, endócrino) | 486 |
| Tipos celulares/rótulos | 573 |
| Grau médio de saída | 6,5 |
| Sinapses por aresta (média) | 15,15 |

O recorte prioriza corpos pedunculados (KCs, MBON, MBIN), neurônios
dopaminérgicos, vias olfatórias/mecanossensoriais/gustativas, complexo central
e saídas motoras. Arestas têm no máximo 90 saídas por neurônio e o asset total
é limitado a 120.000 arestas.

**Correção aplicada na extração:** o pacote de anotações traz
`top_nt = dopamine` para as células de Kenyon, o que é biologicamente
incorreto — KCs são colinérgicas. Sem corrigir, todas as sinapses de KC teriam
sinal modulatório zero e o circuito não propagaria. O script força ACh para
KCs e registra a correção em [extract_connectome.py](tools/extract_connectome.py).

### Simulação

- Neurônios **leaky integrate-and-fire** (LIF): `tau = 20 ms`, limiar `-50 mV`,
  repouso/reset `-65 mV`, refratário `2 ms`, ruído e corrente tônica.
- Atraso sináptico de 1–2 ms; peso derivado de `log1p(nº de sinapses)`, entre
  0,35 e 1,25.
- Sinal por neurotransmissor: ACh excitatória; GABA e glutamato inibitórios;
  dopamina, serotonina e octopamina modulatórias (não propagam potencial).
- **STDP de três fatores**: pares pré/pós dentro de 25 ms geram elegibilidade
  (`A+ = A- = 0,9`, `tau = 18 ms`); a elegibilidade decai com `tau = 260 ms`.
  Um pulso de dopamina converte elegibilidade em mudança de peso
  (`taxa = 0,035`, peso limitado a 0,45×–1,75× do original).
- Recompensa: cada ensaio agenda 3 pulsos; a dopamina é um escalar global que
  também estimula os 331 DANs. Não há codificação por compartimento.
- **Engrama de 512 dimensões**: 80% da assembleia estrutural de KCs
  recrutada pelas sinapses reais da entrada + 20% da atividade observada no
  circuito. É essa assinatura que indexa a memória.
- Texto vira um padrão determinístico de 60 canais sensoriais por hash FNV-1a
  com código distribuído (palavras + pares de palavras). Não há embeddings.

### Recuperação

A pergunta atual é codificada, o circuito roda 160 ms e produz um engrama. Cada
fato guardado é comparado por:

```text
score = 0,60 × similaridade lexical + 0,40 × similaridade de engrama calibrada
```

A similaridade lexical combina Jaccard de termos, cobertura da pergunta e
trigramas de caracteres. A similaridade de engrama é calibrada para que
assembleias distantes não contem. A resposta é aceita se `score >= 0,42` ou se
a similaridade lexical for `>= 0,55`. Caso contrário, o app responde que ainda
não aprendeu.

### Conexões divergentes

Depois de escolher a resposta, o motor propaga ativação por uma rede de
conceitos montada com **todas** as memórias aprendidas
([Concepts.kt](app/src/main/java/com/flymind/brain/Concepts.kt)):

- cada fato é reduzido a até 30 ideias (termos com 3+ letras, sem palavras de
  ligação);
- dois fatos se ligam pelo peso de Jaccard entre as ideias deles; fatos com
  nenhuma ideia em comum não se ligam;
- a ativação inicial vem da pergunta (70% similaridade lexical + 30% cobertura
  de ideias), propaga 2 saltos com fator 0,6 e recebe ainda 0,35 × similaridade
  de engrama calibrada;
- só entram ideias com ativação `>= 0,07`, no máximo 3, e a resposta direta é
  excluída;
- a bolha mostra **CONECTA COM**, a pergunta vizinha e o caminho (`via luz,
  planta`). Tocar na pergunta vizinha já pergunta aquilo à mosca.

O efeito é o de associação divergente: uma pergunta sobre fotossíntese puxa a
memória de clorofila mesmo sem repetir as palavras da resposta principal. Se
nada tiver conceito em comum, o app não inventa ligação — a lista fica vazia.

### Persistência

- [FlyMindDatabase.kt](app/src/main/java/com/flymind/data/FlyMindDatabase.kt):
  SQLite `flymind.db` com as tabelas `facts` e `events` (diário de aprendizado,
  recordação, erro, reconsolidação e esquecimento).
- Cada fato guarda o engrama de 512 floats, estabilidade, disparos, pulsos de
  dopamina e tamanho da assembleia.
- Os pesos plásticos são salvos em `files/connectome_weights.bin` e recarregados
  na abertura.
- **Memória** → exportar gera `memorias_flymind.json` com todos os fatos e
  engramas via compartilhamento do Android.
- Limpar tudo apaga fatos, eventos e o snapshot de pesos.

## Compilar

Pré-requisitos: JDK 17, Android SDK com plataforma 36 e build-tools, Gradle
wrapper incluído.

```bash
# Linux/macOS
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest

# Windows
gradlew.bat :app:assembleDebug
gradlew.bat :app:testDebugUnitTest
```

O `local.properties` do projeto aponta para o SDK desta máquina. Em outra
máquina, ajuste `sdk.dir` ou defina `ANDROID_HOME`.

Saída: `app/build/outputs/apk/debug/app-debug.apk`.

### Testes

`./gradlew :app:testDebugUnitTest` roda 11 testes JVM, todos passando:

- carga do asset real, contagens e 512 dimensões de leitura;
- estimulação produz disparos, engrama e mudança de peso;
- textos diferentes geram engramas diferentes (cosseno 0,31);
- treino entrega 15 pulsos em 5 ensaios, salva snapshot e recupera a pergunta
  exata (score 0,80; lexical 0,68; engrama 0,99);
- pergunta não ensinada não é respondida;
- paráfrase com as mesmas palavras-chave recupera o fato correto (score 0,75
  contra 0,14 do melhor fato errado);
- extração de HTML sem `script`/`style`, com títulos virando seções de texto;
- decodificação de entidades, normalização de URL sem esquema e recusa de `ftp`;
- dois fatos de biologia se conectam por ideias em comum e o fato de futebol
  não é ligado a eles;
- a consulta devolve conexões divergentes sem repetir a resposta direta.

Medições do teste de treino (JVM, não no celular): 67.380 disparos, 15 pulsos de
dopamina, estabilidade 0,993 e mudança média de peso de 12,0%.

## Reextrair o conectoma

Os parquets de origem não são versionados (451 MB). Baixe:

- `connections.parquet` e `nodes.parquet` do pacote FlyWire FAFB v783;
- ou use os arquivos anexados em `Downloads`.

Instale `pyarrow` e `numpy`, depois:

```bash
python tools/extract_connectome.py \
  --nodes "C:/Users/conta/Downloads/nodes.parquet" \
  --connections "C:/Users/conta/Downloads/connections.parquet"
```

O script varre 15 milhões de linhas em blocos, mantém arestas internas com
≥ 5 sinapses, aplica o teto de grau/volume, grava o asset e o relatório
[connectome_stats.json](tools/connectome_stats.json). A extração é
determinística (`seed = 42`).

## Estrutura

```text
app/src/main/java/com/flymind/
  brain/Connectome.kt      leitura do asset, CSR, estatísticas
  brain/SpikingBrain.kt    LIF, STDP, dopamina, engrama
  brain/TextCode.kt        codificação de texto, similaridade, parser do caderno
  brain/LearningEngine.kt  treino, recuperação, snapshot de pesos
  brain/Concepts.kt        ideias, ligações entre fatos e associação divergente
  brain/WebSource.kt       busca da página e extração de trechos aprendíveis
  data/                    modelos e SQLite
  voice/VoiceEngine.kt     TTS, vozes e preferências
  voice/VoiceFilePlayer.kt importa e toca arquivo de voz (IA)
  ui/                      ViewModel, tema, componentes e telas Compose
  ui/ios/Ios.kt            botões, campos, switch, slider, linhas e alerta iOS
  ui/ios/IosChrome.kt      título grande, tab bar e barra de navegação iOS
  ui/theme/Theme.kt        tokens, tipografia Inter e formas do iOS
  res/font/inter_*.ttf     Inter (OFL) embarcada no APK
  hyperos/HyperOs.kt       detecção e atalhos MIUI/HyperOS
  util/ShareUtil.kt        exportação JSON
app/src/main/res/mipmap-*/ic_launcher_bg.png  ícone adaptativo (5 densidades)
tools/extract_connectome.py
tools/make_app_icon.py   gera o ícone a partir da arte original
app/src/main/assets/connectome/flywire_mb.bin
```

## Limites científicos e técnicos

- **Não é o cérebro inteiro.** É um recorte de 6.909 neurônios (4,96% dos
  139.255) e 44.893 arestas. Não modela corpo, músculos, visão real, olfato real
  nem o restante do sistema nervoso.
- **A dopamina é um escalar global.** O app tem 331 neurônios dopaminérgicos e
  os estimula, mas o sinal de recompensa que altera pesos é compartilhado. A
  biologia usa compartimentos e valências distintos.
- **O aprendizado é supervisionado.** Você fornece pares pergunta/resposta ou
  uma URL. O app não descobre sozinho o que é relevante, não faz aprendizado
  por reforço de tentativa e erro, não navega em links e não escolhe temas por
  conta própria.
- **Não é um LLM.** As respostas são recuperação associativa de fatos
  ensinados, não geração livre. Paráfrases com as mesmas palavras-chave
  funcionam; sinônimos sem sobreposição lexical podem falhar.
- **O engrama é uma assinatura engenheirada.** 80% dele vem da estrutura
  recrutada pelas sinapses; 20% vem da atividade observada. Não é uma leitura
  literal e completa do padrão de spikes.
- **O sinal dos neurotransmissores é uma heurística.** ACh excita; GABA e
  glutamato inibem; DA/5-HT/OA modulam. Não há modelagem de receptores,
  neuromodulação por subtipo ou plasticidade inibitória detalhada.
- **"HyperOS integrado" significa integração de app comum.** Não há root, API
  privada, widget, tile de configurações rápidas ou serviço persistente. Os
  atalhos abrem telas do sistema; o resto é tema, ícone, edge-to-edge e
  comportamento de tela.
- **Internet só sob demanda.** O manifesto declara `INTERNET` apenas para
  baixar a página que você cola na aba Ensinar. Nenhum outro dado sai do
  aparelho, não há telemetria, conta ou servidor do app, e os links da página
  não são seguidos. O TTS usa vozes locais ou de rede conforme a voz escolhida
  no sistema.
- **Desempenho.** Rodar 6 ensaios custa cerca de 67 mil disparos na JVM. Em
  celulares intermediários como o Redmi Note 11S, espere segundos por fato e
  minutos para cadernos grandes.

## Licença e citação

Os dados de conectividade são **CC-BY-4.0**. Cite:

1. Dorkenwald et al., *Neuronal wiring diagram of an adult brain*, Nature 2024.
   [doi:10.1038/s41586-024-07558-y](https://doi.org/10.1038/s41586-024-07558-y)
2. Schlegel et al., *Whole-brain annotation and multi-connectome cell typing of
   Drosophila*, Nature 2024.
   [doi:10.1038/s41586-024-07686-5](https://doi.org/10.1038/s41586-024-07686-5)

O Conectoma não é afiliado ao consórcio FlyWire. O recorte, a simulação e o app
são independentes.

A fonte **Inter** está sob a SIL Open Font License 1.1
([licenses/Inter-OFL.txt](licenses/Inter-OFL.txt)). A interface segue as
convenções visuais do iOS; não usa SF Pro (licenciado só para plataformas da
Apple) nem qualquer recurso proprietário da Apple.
