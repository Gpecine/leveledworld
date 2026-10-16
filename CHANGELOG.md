# Changelog

Todos os jars ficam em [`releases/`](releases/) e também na aba **Releases** do GitHub.

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
