# Leveled World

Mod RPG para **Minecraft 1.21.1 (NeoForge)** inspirado no sistema de level do Skyrim:
o personagem evolui usando skills, e **o mundo inteiro escala com o seu level** — não importa para onde você vá.

## Como funciona

### Skills (sobem com o uso)
12 skills em 3 grupos:

| Grupo | Skill | Como ganhar XP |
|---|---|---|
| Guerreiro | Uma Mão | dano com espadas e armas de uma mão |
| Guerreiro | Duas Mãos | dano com machados, maça e tridente |
| Guerreiro | Arquearia | acertar projéteis (arco, besta, tridente) |
| Guerreiro | Bloqueio | bloquear ataques com escudo |
| Guerreiro | Armadura Pesada | receber dano usando ferro, diamante ou netherite |
| Ladrão | Armadura Leve | receber dano usando couro, malha, ouro, tartaruga ou tatu |
| Ladrão | Furtividade | ficar agachado perto de mobs sem ser visto e dar ataques furtivos |
| Ladrão | Atletismo | correr, nadar, escalar, pular e sobreviver a quedas |
| Artesão | Mineração | quebrar minérios (cada minério vale diferente) e pedra |
| Artesão | Lenhador | cortar troncos |
| Artesão | Ferraria | fabricar e reparar na bigorna |
| Artesão | Agricultura | colher plantações maduras, reproduzir animais e pescar |

- Toda skill começa no level **15** e vai até **100**.
- XP para o próximo level da skill: `0.5 × level^1.95 + 10`.
- XP de combate escala com a dificuldade do mob (`level do mob / seu level`, entre ×0,5 e ×2).
- **Anti-farm**: mobs de spawner dão 25% do XP; troncos e minérios colocados por jogadores e minérios quebrados com
  Toque de Seda não dão XP.

### Level do personagem
- Cada vez que uma skill sobe, o personagem ganha XP igual a 75% do novo level da skill.
- XP para o próximo level do personagem: `(level + 3) × 25` (máximo configurável, padrão 250).
- Persiste ao morrer.

### Perks (árvores de constelação)
- Cada level do personagem dá **1 ponto de perk**.
- Cada skill tem uma árvore com **15 perks** (180 no total), alguns com vários ranks. As estrelas de cada árvore
  desenham a silhueta da skill: espada (Uma Mão), machadão de lâmina dupla (Duas Mãos), arco com flecha, escudo,
  peitoral (Pesada), pena (Leve), olho (Furtividade), corredor (Atletismo), picareta, pinheiro (Lenhador),
  bigorna (Ferraria) e espiga de trigo (Agricultura). Para comprar, é preciso ter o
  level de skill exigido e o perk anterior na árvore.
- Na tela **K**, selecione uma skill no carrossel para ver a árvore. Passe o mouse para ver os detalhes e clique para aprender.
- **Skill lendária**: no level 100, o botão "Tornar Lendária" volta a skill ao level 15 e devolve os pontos de perk
  dela. A skill ganha uma ★ e pode subir de novo, dando mais XP de personagem.

### Livros de skill e treinadores
- **Tomos de skill** aparecem em baús (masmorras, templos, fortalezas, cidades antigas...) e na pesca. Cada tomo dá +1 level
  numa skill aleatória. Os tomos "Antigos" são raros e dão +2.
- **Aldeões treinam skills** em troca de esmeraldas (5 por level de personagem, configurável):

| Profissão | Skills |
|---|---|
| Armeiro | Uma Mão, Duas Mãos |
| Fabricante de armaduras | Armadura Pesada, Armadura Leve, Bloqueio |
| Flecheiro | Arquearia |
| Ferramenteiro | Mineração, Lenhador, Ferraria |
| Fazendeiro / Açougueiro | Agricultura |
| Cartógrafo | Atletismo |
| Mercador ambulante | Furtividade |

  O level máximo que um aldeão ensina depende do nível dele (de novato a mestre: 25 / 40 / 55 / 65 / 75).

### Atributos (Vida / Stamina / Mana)
Cada level do personagem dá **1 ponto** para investir na tela **K**:

| Atributo | Por ponto | Uso |
|---|---|---|
| Vida | +½ coração (1 coração a cada 2 pontos) | vida máxima |
| Stamina | +10 (base 100), +5% de recuperação e +0,5% de velocidade de corrida (máx. +15%) | correr (12/s), pular (5), atacar (8; ×1,5 com armas de duas mãos) e atirar com arco (6). Recupera 12/s após 1s |
| Mana | +10 (base 100) | regenera 3/s; será usada pelo sistema de magias |

**Exaustão**: se a stamina acabar, o personagem fica exausto (barra vermelha) até a barra **encher de novo**.
A barra vermelha enche com o **dobro da velocidade** da stamina normal: se a stamina enche em 2s, ela enche em 1s.
Enquanto isso, ele não corre nem ataca, o arco dispara fraco, e ele anda e quebra blocos na **metade da velocidade**.
A redução vale para a mão e para qualquer ferramenta, sobre a velocidade final (já com Eficiência e Pressa).
Os valores ficam em `[stamina]` na config do servidor.

### Mundo escalado
Todo mob hostil, ao surgir, recebe um level baseado no **jogador mais próximo** (raio 128):

```
level do mob = level do jogador × aleatório(0.8 … 1.2)
```

- Bosses (Wither, Ender Dragon, Warden, Elder Guardian) têm level mínimo 20.
- Bônus por level acima de 1: **+5% vida**, **+3% dano**, **+0.2 armadura**.
- O level fica salvo no mob — recarregar o chunk não muda nada.
- Sem jogador por perto, usa a média de level dos jogadores da dimensão (ou 1 se não houver ninguém).

### Tiers de mobs
Mobs agressivos e animais nascem com um tier: **Comum, Incomum, Raro, Épico, Lendário ou Exótico** (60% / 25% / 10% /
4% / 0,9% / 0,1% na base; os tiers altos ficam mais comuns com o level e no Nether/End).
- **Agressivos**: um pouco mais fortes (+5% a +40% de vida e dano), com mais drops, mais XP e loot extra (do loot
  incomum até netherite, totem e tomos no Exótico).
- **Animais**: mais produtos marcados com o tier (couro, lã, carne, ovos, leite), que rendem comida melhor,
  equipamento mais forte e mais esmeraldas com aldeões. Cavalos e lobos de tier alto são melhores.
- **Reprodução**: o filhote herda o tier dos pais (90% igual, 9% sobe, 1% desce).
- Detalhes no [CHANGELOG](CHANGELOG.md#090--tiers-de-mobs).

### Dimensões
| Dimensão | Mobs | Recompensas |
|---|---|---|
| Superfície | level normal | normais |
| Nether | +5 levels (mínimo 10) | loot ×1,5, raridade +10, 15% de loot exclusivo do Nether |
| End | +10 levels (mínimo 20) | loot ×2, raridade +20, 20% de loot exclusivo do End |

Configurável em `[dimensions]` na config do servidor, incluindo dimensões de outros mods.

### Recompensas escaladas
- Orbes de XP **+5% por level** do mob e drops empilháveis **+2% por level**.
- **Loot bônus** ao matar mobs: chance de 1% por level (máx. 50%).
  - Level < 15: comum (pepitas, carvão, flechas...)
  - Level 15–34: incomum, brilha (lingotes, esmeraldas, livros e equipamento de ferro encantados)
  - Level 35+: raro, brilha e toca um som (diamantes, equipamento de diamante encantado, maçãs douradas, netherite scrap)
- As tabelas ficam em `data/leveledworld/loot_table/bonus/` e podem ser sobrescritas por datapack.

### Interface
- **HUD**: painel dourado com medalhão de level, barras de XP (dourada), stamina (verde) e mana (azul), e selo `+N` de pontos livres. Aviso de **LEVEL UP** ao subir.
- **Tecla `K`**: céu de constelações (uma por skill, cada uma com sua região do espaço como fundo) para girar arrastando.
  A skill selecionada mostra sua árvore de perks. Embaixo fica o painel do personagem, com Mana / Vida / Stamina,
  botões "+" para gastar pontos e os selos de pontos de atributo (dourado) e de perk (roxo).
- **Tecla de ação em área** (Opções → Controles → Leveled World): define a tecla usada para minerar o veio inteiro,
  derrubar a árvore inteira e colher em área 3×3. Sem tecla (padrão), essas ações usam o agachar (Shift).
- **Olho de furtividade** acima da mira quando agachado: fechado (oculto), entreaberto (alguém desconfia) ou aberto (visto).
- **Placa nos mobs**: medalhão com o level + nome + barra de vida. A cor do medalhão indica a dificuldade
  em relação a você: cinza (muito mais fraco), verde, dourado (equivalente), laranja, vermelho com ☠ (muito mais forte).
  Configurável em `config/leveledworld-client.toml` (ligar/desligar, distância, barra de vida).

### Comandos
| Comando | Permissão |
|---|---|
| `/rpg info [jogador]` | todos |
| `/rpg level set <jogador> <level>` | OP |
| `/rpg skill set <jogador> <skill> <level>` | OP |
| `/rpg skill addxp <jogador> <skill> <xp>` | OP |
| `/rpg points add <jogador> <n>` / `/rpg points reset <jogador>` | OP |
| `/rpg perks reset <jogador>` / `/rpg perks points <jogador> <n>` (adiciona pontos) | OP |
| `/rpg perks give <jogador> <perk> [rank]` — dá um perk direto, ex.: `mining.mineracao_veio` | OP |
| `/rpg tome <jogador> <skill> [levels]` — dá um tomo de skill | OP |
| `/rpg stamina drain <jogador>` / `/rpg stamina refill <jogador>` — zera (deixa exausto) ou enche a stamina | OP |
| `/rpg mob <entidade>` — mostra o level e o tier de um mob | OP |
| `/rpg mobtier <entidades> <tier>` — define o tier (common, uncommon, rare, epic, legendary, exotic) | OP |

## Configuração
`config/leveledworld-server.toml` (pode ser sobrescrito por mundo em `saves/<mundo>/serverconfig/`) — multiplicador de XP, levels máximos,
raio de busca, faixa de level dos mobs (min/max factor), bônus de atributos por level, pontos de perk por level,
multiplicador de XP por skill (`skillXpMultipliers`, ex.: `"mining=0.8"`), anti-farm e treinadores (`[training]`).

## Arte
Quer criar texturas, modelos, mobs ou sons para o mod? Veja o [tutorial de arte](docs/tutorial-arte.md).
Arquivos de arte novos entram em [`arte/`](arte/).

## Download
Os jars de todas as versões ficam em [`releases/`](releases/) e na aba **Releases** do GitHub.
Histórico de mudanças: [CHANGELOG.md](CHANGELOG.md).

## Build e teste
Requer Java 21.
```bash
./gradlew build        # gera build/libs/leveledworld-<versão>.jar
./gradlew runClient    # abre o Minecraft com o mod
```

Roteiro de teste rápido:
1. `/rpg level set @s 20` e depois `/summon zombie` → `/rpg mob @e[type=zombie,limit=1,sort=nearest]` deve mostrar level ~16–24.
2. `/data get entity @e[type=zombie,limit=1,sort=nearest] Attributes` mostra os modificadores `leveledworld:level_*`.
3. Bater em mobs / minerar → mensagens de skill subindo, barra do HUD enche; `K` abre o status.
4. Morrer e relogar → level e skills mantidos.

## Próximos passos (ideias)
- Sistema de magias usando a mana.

## Publicar uma nova versão
1. Altere `mod_version` em `gradle.properties` e adicione a seção no `CHANGELOG.md`.
2. Copie o jar gerado para `releases/`.
3. Crie e envie a tag: `git tag v0.X.0 && git push origin v0.X.0` — o GitHub Actions cria a Release com o jar.
