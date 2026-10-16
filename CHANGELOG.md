# Changelog

Todos os jars ficam em [`releases/`](releases/) e também na aba **Releases** do GitHub.

## 0.4.0
### Visual novo (estilo RPG de fantasia)
- **HUD**: painel com moldura dourada, medalhão com o level e barras com gradiente/brilho/marcações (XP dourado, stamina verde, mana azul). Selo dourado pulsante `+N` quando há pontos livres.
- **Aviso de LEVEL UP** no mesmo estilo, abaixo do HUD, por 4 segundos.
- **Tela K** redesenhada no mesmo visual: medalhão, selo de pontos, ícones dos atributos, barras com gradiente e botões "+" dourados.
- **Placas dos mobs**: medalhão com o level na cor da dificuldade (cinza → verde → dourado → laranja → vermelho com ☠), nome claro, moldura dourada e barra de vida com gradiente (verde/amarelo/vermelho).

### Loot e XP escalados pelo level do mob
- Orbes de XP: +5% por level do mob.
- Drops empilháveis: +2% de quantidade por level.
- **Loot bônus** (só se o jogador participou da morte): chance de 1% por level do mob (máx. 50%). Raridade pela faixa de level: comum (<15), incomum (15–34, item brilha) e raro (35+, item brilha + som). Tabelas editáveis em `data/leveledworld/loot_table/bonus/{common,uncommon,rare}.json`.
- Tudo configurável na seção `[rewards]` de `leveledworld-server.toml`.

### Correções
- XP de skill por dano agora é limitado à vida máxima da vítima (um golpe gigante não rende XP absurdo).

## 0.3.1
- **Corrige crash** ao renderizar um mob escalado (`IllegalStateException: Not building!`): a barra de vida era escrita num buffer de vértices já encerrado pelo desenho do texto.
- **Corrige a placa dos mobs**: o fundo estava sendo desenhado na frente do texto (profundidade com sinal invertido), escondendo o nome. Agora fundo e barra ficam atrás do texto.
- Testado em cliente real: placas, tela K, botões de atributo e gasto de stamina.
- Dev: `./gradlew runClient -PquickPlay=<mundo>` abre direto um mundo de `run/saves`.

## 0.3.0
- **Pontos de atributo estilo Skyrim**: cada level do personagem dá 1 ponto para gastar em **Vida**, **Stamina** ou **Mana** (tela K, botões "+").
  - Vida: +1 coração por ponto.
  - Stamina: 100 + 10 por ponto. Correr gasta 10/s, pular 4, atacar 3; regenera 15/s após 1s sem gastar. Sem stamina não dá para correr.
  - Mana: 100 + 10 por ponto, regenera 3/s (base para o futuro sistema de magias).
  - Ao investir num atributo, ele é restaurado por completo.
- HUD mostra barras de stamina (verde) e mana (azul) e um aviso `+N` quando há pontos livres. Barra de XP agora é dourada.
- Tela K redesenhada em duas colunas: personagem/atributos e skills.
- Comandos `/rpg points add <jogador> <n>` e `/rpg points reset <jogador>`.
- **Correção**: o nome acima dos mobs desaparecia parcialmente à distância (z-fighting entre o texto e o fundo).
- Mundos de versões anteriores continuam compatíveis.

## 0.2.0
- Placa acima dos mobs escalados: **Nome Lv. X** + barra de vida (verde → vermelho).
- Cor do level conforme a diferença para o seu level: cinza (muito mais fraco), verde, branco, laranja, vermelho (muito mais forte).
- Só aparece com linha de visão e até 24 blocos; configurável em `config/leveledworld-client.toml`.

## 0.1.0
- Skills estilo Skyrim (Combate, Arquearia, Bloqueio, Armadura, Mineração, Lenhador, Ferraria) que sobem com o uso.
- Level do personagem alimentado pelas skills; persiste ao morrer.
- Mobs hostis escalam com o level do jogador mais próximo (80%–120%), bosses com mínimo 20.
- HUD com level e barra de XP; tela de status na tecla K.
- Comandos `/rpg`.
