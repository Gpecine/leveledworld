# Changelog

Todos os jars ficam em [`releases/`](releases/) e também na aba **Releases** do GitHub.

## 0.7.0
### Dificuldade e loot por dimensão
| Dimensão | Level dos mobs | Mínimo | Raridade do loot | Chance de loot e XP | Loot exclusivo |
|---|---|---|---|---|---|
| Superfície | normal | — | normal | ×1 | — |
| **Nether** | +5 | 10 | +10 níveis | ×1,5 | 15%: quartzo, ouro, blaze rod, glowstone, nether wart, magma cream, lágrima de ghast, espada de ouro encantada, ancient debris, netherite scrap |
| **End** | +10 | 20 | +20 níveis | ×2 | 20%: ender pearl, diamante, frascos de XP, chorus, livro encantado, shulker shell, dragon breath, espada/elmo de diamante encantados, end crystal |
- "Raridade do loot" soma ao level usado para escolher comum/incomum/raro: no Nether o loot incomum aparece bem mais cedo e no End o raro.
- Loot exclusivo brilha e toca um som próprio.
- Aviso ao entrar numa dimensão perigosa, com o bônus de level e de recompensas.
- Tudo configurável em `[dimensions] rules` de `leveledworld-server.toml`, inclusive para **dimensões de outros mods**
  (formato `dimensão,bônusDeLevel,levelMínimo,bônusDeLoot,multiplicadorDeRecompensa,chanceExclusiva`). Para dar loot
  exclusivo a uma dimensão nova, basta um datapack com `data/leveledworld/loot_table/dimension/<namespace>/<dimensão>.json`.

## 0.6.2
- Combate (pilares de poeira) e Ferraria (supernova) também ganharam fundos em alta resolução (1600×900, saturação −8%).
  Agora as 7 constelações usam a arte do autor.

## 0.6.1
- Fundos da tela de constelações trocados por **imagens em alta resolução (1600×900)** fornecidas pelo autor:
  galáxia espiral (Arquearia), nebulosa azul com aglomerado (Bloqueio), nebulosa vermelha (Armadura),
  campo de asteroides dourado (Mineração) e nebulosa "olho" verde (Lenhador). Combate (pilares de poeira)
  e Ferraria (supernova) usam a arte de referência do autor.
- Saturação levemente reduzida (−8%) para combinar com a interface.
- O jar ficou maior (~13 MB) por causa das imagens.

## 0.6.0
### Fundos de espaço na tela de constelações
- Cada skill tem sua própria região do espaço como fundo, gerada de forma procedural e original (`tools/gen_skybox.py`):
  - Combate: **pilares de poeira** (inspirados nos Pilares da Criação)
  - Arquearia: galáxia espiral · Bloqueio: nebulosa azul com aglomerado · Armadura: nebulosa de emissão vermelha
  - Mineração: nuvem de poeira dourada · Lenhador: nebulosa planetária verde · Ferraria: remanescente de supernova
- Ao girar o carrossel, o fundo desliza com parallax e se funde suavemente com o da próxima skill (o fundo segue o
  carrossel com um leve atraso, então nada "salta" mesmo arrastando rápido). O fundo também deriva lentamente parado.
- Animações da tela agora independentes do FPS.

## 0.5.0
### Perks por skill (constelações)
- Cada skill tem **4 perks** desbloqueados automaticamente nos níveis 25, 50, 75 e 100:
  - **Combate**: +10% / +20% dano corpo-a-corpo, ataques gastam 50% menos stamina, 15% de crítico (×1,5).
  - **Arquearia**: +10% / +20% / +30% dano à distância, 15% de crítico (×1,5).
  - **Bloqueio**: bloquear recupera stamina, devolve 25% do dano, empurra o atacante, devolve 50%.
  - **Armadura**: até 5% / 10% / 15% / 20% menos dano (proporcional às peças).
  - **Mineração**: +15% / +30% velocidade com picareta, 10% / 20% de minério dobrado (sem efeito com Toque de Seda).
  - **Lenhador**: +20% / +40% velocidade com machado, 15% / 30% de tronco extra.
  - **Ferraria**: 10% / 20% / 30% / 40% de chance do equipamento fabricado sair encantado (cada vez mais forte).
- Mensagem e som próprio ao desbloquear um perk.

### Tela K nova (inspirada no Skyrim)
- Céu estrelado com uma **constelação por skill** no formato do símbolo (espada, arco, escudo, elmo, picareta, machado, bigorna).
- Carrossel: **arraste para os lados**, setas, A/D, roda do mouse ou clique numa constelação lateral.
- As estrelas grandes são os perks (douradas quando desbloqueadas); passe o mouse para ver o efeito.
- Painel inferior: level, XP e **Mana / Vida / Stamina** (ordem do Skyrim) com botões "+" que só aparecem quando há pontos.
- O HUD some enquanto a tela está aberta.

### Ajustes pedidos
- **Som de level up novo e original** (tambor, trompas que resolvem para acorde maior, coro e sinos), tocado só para o jogador.
- **Um único som** mesmo subindo vários levels de uma vez (antes duplicava e ficava alto).
- **Vida: +4 HP (2 corações) por ponto** (antes 2). A chave da config mudou para `healthPerAttributePoint` para o novo valor valer em configs existentes.
- **Stamina agora também dá +0,5% de velocidade de corrida por ponto** (máx. +15%), só enquanto corre.
- XP de personagem por level de skill reduzido para **75%** (`characterXpPerSkillLevel`).

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
