# Changelog

Todos os jars ficam em [`releases/`](releases/) e também na aba **Releases** do GitHub.

## 0.8.1
### Árvores de perks com a silhueta da skill
- As 15 estrelas de cada árvore agora desenham o símbolo da skill, e as linhas entre elas são os requisitos:

| Skill | Silhueta |
|---|---|
| Uma Mão | espada (pomo → empunhadura → guarda → lâmina → ponta) |
| Duas Mãos | machadão de lâmina dupla |
| Arquearia | arco tensionado com flecha |
| Bloqueio | escudo com cruz |
| Armadura Pesada | peitoral |
| Armadura Leve | pena |
| Furtividade | olho |
| Atletismo | corredor |
| Mineração | picareta |
| Lenhador | pinheiro |
| Ferraria | bigorna |
| Agricultura | espiga de trigo |

- As constelações laterais do carrossel usam o mesmo desenho.
- Alguns requisitos e níveis mudaram para seguir o desenho, sempre subindo de nível ao longo de cada caminho.
  Os perks já comprados continuam comprados.

### Vida
- Cada ponto em Vida dá **½ coração** (1 coração a cada 2 pontos), em vez de 2 corações.

### Stamina
- **Exaustão**: se a stamina acabar, o personagem fica exausto e a barra fica vermelha. Ele não corre nem ataca,
  e o arco dispara fraco, até recuperar 30% da stamina.
- Ações gastam mais: correr 12/s (antes 10), pular 5 (antes 4), atacar 8 (antes 3; ×1,5 com armas de duas mãos)
  e atirar com arco 6 (antes não gastava).
- Cada ponto em Stamina acelera a recuperação em +5% (20 pontos = o dobro). A base é 12/s.
- As opções de stamina foram para uma seção própria, `[stamina]`, na config do servidor. A vida usa a chave nova
  `maxHealthPerPoint`. Configs antigas recebem os valores novos automaticamente.

## 0.8.0 — Skills 2.0
### 12 skills
- **Combate** virou **Uma Mão** (espadas) e **Duas Mãos** (machados, maça, tridente).
- **Armadura** virou **Armadura Pesada** (ferro, diamante, netherite) e **Armadura Leve** (couro, malha, ouro, tartaruga, tatu).
  Armaduras de outros mods são classificadas pela defesa do peitoral.
- Skills novas: **Furtividade**, **Atletismo** e **Agricultura**.
- As skills estão agrupadas em Guerreiro, Ladrão e Artesão. As 5 novas constelações têm símbolos e fundos próprios,
  com as imagens do autor: Duas Mãos, Furtividade, Atletismo, Armadura Leve e Agricultura.

### Árvores de perks com pontos
- **180 perks** (15 por skill), muitos com vários ranks. Cada level de personagem dá **1 ponto de perk**.
  Para comprar um perk, é preciso ter o level de skill exigido e o perk anterior na árvore.
- A árvore aparece sobre a constelação da skill selecionada, com estrelas em 5 estados: comprado, no máximo,
  disponível, falta level e bloqueado. Tooltip com o efeito de cada rank e popup de confirmação.
- Destaques:
  - Uma Mão: críticos, sangramento e execução.
  - Duas Mãos: golpe em área, penetração de armadura e ataque carregado.
  - Arquearia: disparo duplo, chuva de flechas e flecha paralisante.
  - Bloqueio: Refletir/Espinhos, golpe de escudo e Fortaleza Imortal.
  - Armadura Pesada: Juggernaut, Colosso (+vida) e Imparável.
  - Armadura Leve: esquiva, rolamento e esquiva de projéteis.
  - Furtividade: ataque furtivo até ×3, Desaparecer, Fantasma, e Endermen e sculk que ignoram você.
  - Atletismo: corrida, pulo, queda, natação, escalada e Segundo Fôlego.
  - Mineração: Faro de Minério (realça minérios próximos), mineração em veio, fundição automática e gemas.
  - Lenhador: derrubar árvore inteira, replantio automático e Pomar Dourado.
  - Ferraria: itens forjados com **qualidade** (Fino / Superior / Obra-prima, com assinatura), reciclagem e bigorna mais barata.
  - Agricultura: replantio, colheita em área, aura de crescimento, gêmeos na reprodução e Safra Dourada.
- Novo efeito **Sangramento**.
- Na tela K aparecem os selos de pontos de atributo (dourado) e de pontos de perk (roxo).

### Skills lendárias
- No level 100, o botão **Tornar Lendária** volta a skill ao level 15, devolve os pontos de perk dela e dá uma ★.
  A skill pode subir de novo e continuar dando XP de personagem.
- O level máximo do personagem agora é configurável (`maxCharacterLevel`, padrão 250).

### Livros de skill e treinadores
- **Tomos de skill** em baús de masmorras, templos, fortalezas, cidades antigas, bastiões, End City e na pesca:
  +1 level numa skill sorteada. Os tomos "Antigos" são raros e dão +2.
- **Aldeões treinam skills** em troca de esmeraldas, até um level que depende do nível do aldeão (25–75).
  O mercador ambulante ensina Furtividade. O limite é de 5 lições por level de personagem.

### XP balanceado e anti-farm
- O XP de combate escala com a dificuldade do mob (×0,5 a ×2) e matar um mob dá um bônus.
- Cada minério dá um XP diferente; Toque de Seda não dá XP.
- Mobs de spawner dão 25% do XP. Troncos e minérios colocados por jogadores não dão XP.
- Multiplicador de XP por skill na config (`skillXpMultipliers`).

### Interface
- Olho de furtividade acima da mira quando agachado: fechado (oculto), entreaberto ou aberto (visto).
- Mensagem "Ataque furtivo! ×N" na barra de ação.

### Migração de mundos antigos
- A antiga skill Armadura vira Armadura Pesada e Armadura Leve com o mesmo level.
- Os 4 perks automáticos antigos foram substituídos pelas árvores. Personagens existentes recebem `(level − 1)` pontos de perk.

### Comandos
- `/rpg perks reset <jogador>`, `/rpg perks points <jogador> <n>`, `/rpg tome <jogador> <skill> [levels]`.

> Os perks são definidos em código (`skill/PerkTrees.java`), não em JSON como o plano previa: assim fica mais fácil
> validar a árvore e o layout.

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
