# Changelog

Todos os jars ficam em [`releases/`](releases/) e também na aba **Releases** do GitHub.

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
