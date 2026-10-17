# Leveled World

Mod RPG para **Minecraft 1.21.1 (NeoForge)** inspirado no sistema de level do Skyrim:
o personagem evolui usando skills, e **o mundo inteiro escala com o seu level** — não importa para onde você vá.

## Como funciona

### Skills (sobem com o uso)
| Skill | Como ganhar XP |
|---|---|
| Combate | causar dano corpo-a-corpo |
| Arquearia | acertar projéteis (arco, besta, tridente) |
| Bloqueio | bloquear ataques com escudo |
| Armadura | receber dano de criaturas usando armadura (mais peças = mais XP) |
| Mineração | quebrar minérios (muito) e pedra (pouco) |
| Lenhador | cortar troncos |
| Ferraria | fabricar armas, ferramentas, armaduras e escudos |

- Toda skill começa no level **15** e vai até **100**.
- XP para o próximo level da skill: `0.5 × level^1.95 + 10`.

### Level do personagem
- Cada vez que uma skill sobe, o personagem ganha XP igual a 75% do novo level da skill.
- XP para o próximo level do personagem: `(level + 3) × 25`.
- Persiste ao morrer.

### Perks (constelações)
Cada skill desbloqueia 4 perks nos níveis 25, 50, 75 e 100 — veja a lista completa no [CHANGELOG](CHANGELOG.md#050)
ou passe o mouse sobre as estrelas grandes na tela **K**.

### Atributos (Vida / Stamina / Mana)
Cada level do personagem dá **1 ponto** para investir na tela **K**:

| Atributo | Por ponto | Uso |
|---|---|---|
| Vida | +2 corações | vida máxima |
| Stamina | +10 (base 100) e +0,5% de velocidade de corrida (máx. +15%) | correr (10/s), pular (4) e atacar (3); regenera 15/s após 1s. Sem stamina, não dá para correr |
| Mana | +10 (base 100) | regenera 3/s; será usada pelo sistema de magias |

### Mundo escalado
Todo mob hostil, ao surgir, recebe um level baseado no **jogador mais próximo** (raio 128):

```
level do mob = level do jogador × aleatório(0.8 … 1.2)
```

- Bosses (Wither, Ender Dragon, Warden, Elder Guardian) têm level mínimo 20.
- Bônus por level acima de 1: **+5% vida**, **+3% dano**, **+0.2 armadura**.
- O level fica salvo no mob — recarregar o chunk não muda nada.
- Sem jogador por perto, usa a média de level dos jogadores da dimensão (ou 1 se não houver ninguém).

### Recompensas escaladas
- Orbes de XP **+5% por level** do mob e drops empilháveis **+2% por level**.
- **Loot bônus** ao matar mobs: chance de 1% por level (máx. 50%).
  - Level < 15: comum (pepitas, carvão, flechas...)
  - Level 15–34: incomum, brilha (lingotes, esmeraldas, livros e equipamento de ferro encantados)
  - Level 35+: raro, brilha e toca um som (diamantes, equipamento de diamante encantado, maçãs douradas, netherite scrap)
- As tabelas ficam em `data/leveledworld/loot_table/bonus/` e podem ser sobrescritas por datapack.

### Interface
- **HUD**: painel dourado com medalhão de level, barras de XP (dourada), stamina (verde) e mana (azul), e selo `+N` de pontos livres. Aviso de **LEVEL UP** ao subir.
- **Tecla `K`**: céu de constelações (uma por skill, cada uma com sua região do espaço como fundo) para girar arrastando, com os perks de cada skill, e o painel do personagem com Mana / Vida / Stamina e botões "+" para gastar pontos.
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
| `/rpg mob <entidade>` — mostra o level de um mob | OP |

## Configuração
`config/leveledworld-server.toml` (pode ser sobrescrito por mundo em `saves/<mundo>/serverconfig/`) — multiplicador de XP, levels máximos,
raio de busca, faixa de level dos mobs (min/max factor) e bônus de atributos por level.

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
- Bônus de dificuldade por dimensão (Nether/End).

## Publicar uma nova versão
1. Altere `mod_version` em `gradle.properties` e adicione a seção no `CHANGELOG.md`.
2. Copie o jar gerado para `releases/`.
3. Crie e envie a tag: `git tag v0.X.0 && git push origin v0.X.0` — o GitHub Actions cria a Release com o jar.
