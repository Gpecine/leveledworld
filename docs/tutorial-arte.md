# Tutorial de arte do Leveled World

Guia completo para você criar a arte do mod: itens, blocos, minérios, mobs, personagens, armaduras, efeitos e sons.
A divisão do trabalho é simples: **você cria a arte e eu ligo tudo no código** (registro, modelos, receitas, geração
no mundo, IA dos mobs, traduções). Este guia mostra o que instalar, as regras técnicas do Minecraft 1.21.1, o passo a
passo de cada tipo de arte e como me entregar os arquivos.

---

## 1. Equipamento

- **Computador**: qualquer PC que roda o Minecraft roda estes programas.
- **Mouse**: suficiente para pixel art e para o Blockbench. Muitos artistas profissionais de pixel art usam só mouse.
- **Mesa digitalizadora (opcional)**: ajuda em artes maiores, como fundos e capas. Para começar, as de entrada
  da Huion, XP-Pen ou Wacom (One/Intuos S) bastam. Não é necessária para texturas 16×16.

---

## 2. Aplicativos

### 2.1 Pixel art: texturas de itens, blocos, minérios, ícones e telas

| App | Preço | Plataforma | Comentário |
|---|---|---|---|
| **Aseprite** | Pago (≈ US$ 20, compra única; Steam ou site) | Win/Mac/Linux | **O melhor para pixel art.** Camadas, animação, paletas, modo tile (ótimo para blocos que se repetem). O código é aberto: quem sabe compilar pode usar de graça. |
| **LibreSprite** | Grátis | Win/Mac/Linux | Versão livre de uma versão antiga do Aseprite. Muito parecido, sem o modo tile. |
| **Pixelorama** | Grátis | Win/Mac/Linux/navegador | Moderno, tem modo tile e animação. Ótima alternativa gratuita. |
| **Piskel** | Grátis | Navegador | Simples e rápido para itens 16×16 e animações curtas. Bom para começar hoje. |
| **Krita** | Grátis | Win/Mac/Linux | Pintura digital completa. Use para fundos e artes grandes (como os fundos das constelações). |
| **GIMP** / **Paint.NET** | Grátis | GIMP: todas; Paint.NET: Windows | Edição geral: redimensionar, ajustar cor, converter. |
| **Pyxel Edit** | Pago (≈ US$ 9) | Win/Mac | Focado em tiles; bom para blocos. |
| **Photoshop** | Assinatura mensal | Win/Mac | Funciona, mas é caro e não foi feito para pixel art. Só vale se você já usa. |

### 2.2 Modelos 3D: blocos com formato, itens 3D, mobs, personagens e armaduras

| App | Preço | Comentário |
|---|---|---|
| **Blockbench** | Grátis (blockbench.net; também roda no navegador) | **O padrão da comunidade de mods.** Modela com cubos no estilo Minecraft, pinta a textura direto no modelo, anima e exporta nos formatos que o mod usa. Tem plugins, como o do GeckoLib para animações. |
| **Blender** | Grátis | Não é necessário para o mod. Útil só para renders e imagens de divulgação. |

### 2.3 Sons

| App | Preço | Comentário |
|---|---|---|
| **Audacity** | Grátis | Gravar, cortar, limpar ruído e **exportar em .ogg**, o formato do Minecraft. |
| **ChipTone**, **jsfxr**/**Bfxr** | Grátis (navegador) | Geram efeitos curtos: golpes, magia, coleta, level up. |
| **LMMS** | Grátis | Composição musical completa. |
| **Reaper** | Pago (≈ US$ 60, licença pessoal; avaliação de 60 dias) | Editor profissional e leve. |
| **FL Studio** | Pago | Música mais elaborada. |
| freesound.org | Grátis | Banco de sons. **Use só os de licença CC0**, ou dê o crédito que a licença pedir. |

### 2.4 Apoio

- **Lospec** (lospec.com): milhares de paletas de pixel art prontas e tutoriais.
- **GitHub Desktop** (grátis): para colocar seus arquivos direto no repositório do mod, sem usar linha de comando.

### 2.5 Kits recomendados

- **100% grátis**: Pixelorama (ou LibreSprite) + Blockbench + Krita + Audacity + ChipTone.
- **Melhor custo-benefício**: **Aseprite** (único pago, ≈ US$ 20) + Blockbench + Krita + Audacity.
- **Profissional**: Aseprite + Blockbench + Krita + Reaper.

> Os preços são aproximados; confira no site antes de comprar.

---

## 3. Regras técnicas do Minecraft 1.21.1

### 3.1 Arquivos

- **Imagens em PNG** com transparência (RGBA).
- **Nomes de arquivo**: só minúsculas, números e `_`. Sem espaço, acento ou maiúscula. Exemplo: `rubi_bruto.png`,
  e não `Rubi Bruto.png`.
- **Sons em `.ogg`** (Ogg Vorbis). Use **mono** para sons que vêm de um ponto do mundo (golpe, mob); em estéreo o
  som não diminui com a distância. Estéreo só para música ou sons de interface.

### 3.2 Tamanhos

| Tipo | Tamanho | Observação |
|---|---|---|
| Item | 16×16 | Fundo transparente. |
| Bloco / minério | 16×16 por face | Precisa **repetir sem emenda** (tileável). |
| Textura animada | 16×(16×N) | Quadros empilhados na vertical + arquivo `.mcmeta` (veja 4.6). |
| Mob / personagem | 64×64, 64×32 ou 128×128 | Segue o mapa UV do modelo; o Blockbench gera o layout. |
| Armadura | 64×32 por camada | `_layer_1`: elmo, peitoral e botas; `_layer_2`: calças. |
| Ícone de efeito (ex.: Sangramento) | 18×18 | |
| Fundos e telas | Livre | Os fundos das constelações usam 1600×900. |

Dá para usar 32×32 ou 64×64 (estilo "HD"), mas **escolha um tamanho e use no mod inteiro**. Misturar resoluções
fica feio ao lado dos blocos e itens vanilla. Para combinar com o Minecraft, fique em 16×16.

### 3.3 Transparência

- **Blocos sólidos e itens**: cada pixel é 100% opaco ou 100% transparente. Pixels meio transparentes ficam
  estranhos ou viram preto.
- **Vidro, gelo e cristais**: semitransparência é possível, mas me avise, porque o bloco precisa de um modo de
  renderização especial no código.

---

## 4. Passo a passo por tipo de arte

### 4.1 Item simples (gema, lingote, comida, poção, pergaminho)

1. No Aseprite/Pixelorama: **Novo arquivo → 16×16**, fundo transparente.
2. Escolha uma paleta (veja a seção 6) e desenhe a **silhueta** com uma cor só. Ela precisa ser reconhecível em
   tamanho real; dê zoom para fora e confira.
3. Adicione de 3 a 4 tons por material: base, sombra, luz e brilho. **A luz vem do canto superior esquerdo**,
   como no Minecraft.
4. Contorne com um tom escuro da própria cor, não preto puro (preto puro só em pontos de máximo contraste).
5. **Exportar → PNG**, com nome no padrão (`rubi.png`).

**Eu faço**: o registro do item, o modelo (`models/item/rubi.json`), receitas, tradução, onde ele aparece (loot,
mobs, aldeões) e o efeito, se tiver.

### 4.2 Bloco e minério

1. **16×16 tileável**. No Aseprite, ative **View → Tiled Mode → Both** para ver o bloco repetido e corrigir as
   emendas. O Pixelorama tem o mesmo modo.
2. Blocos não têm sombra direcional forte. A "luz" é suave, porque o jogo já sombreia cada face.
3. **Minério** = pedra de fundo + manchas do mineral. Faça as duas versões, **pedra** e **ardósia** (deepslate),
   para o minério aparecer nas duas camadas do mundo. Desenhe a sua pedra no seu estilo, em vez de copiar a
   vanilla.
4. Bloco com faces diferentes (como um tronco): uma textura por face, por exemplo `topo` e `lado`.
5. Faça também os itens que o bloco dá: o minério de rubi dá `rubi_bruto` (item, 16×16).

**Você decide e me diz**: a dureza, a ferramenta necessária (picareta de pedra, ferro ou diamante), o que dropa, e
a **geração no mundo**: altura mínima e máxima, quantos veios por chunk, tamanho do veio e em quais biomas ou
dimensões.

**Eu faço**: blockstate, modelos, loot table, ferramenta mínima, geração no mundo, XP de Mineração e integração
com os perks (Faro de Minério, Veio Rico, Fundidor...).

### 4.3 Bloco com formato especial (altar, bigorna, estante, estátua)

1. No Blockbench: **File → New → Java Block/Item**.
2. Modele com cubos dentro da grade de 16×16×16 (um bloco). Pode passar um pouco da grade, mas evite.
3. **Paint**: crie a textura em **File → New Texture** (ou "Create Texture" no painel) e pinte direto no modelo.
4. **File → Export → Export Block/Item Model** gera o `.json`; salve a textura PNG.
5. Me mande **o `.bbmodel` (o arquivo do projeto), o `.json` e o PNG**.

### 4.4 Item 3D (arma grande, cajado, escudo diferente)

1. Blockbench: **Java Block/Item**. Modele o item.
2. Na aba **Display**, ajuste como ele aparece **na mão** (primeira e terceira pessoa), **no inventário (GUI)**,
   **no chão** e **na moldura**. Isso evita espada gigante na mão ou ícone torto no inventário.
3. Desenhe também um ícone 2D 16×16, que pode ser usado no inventário se você preferir.
4. Exporte e me mande os mesmos 3 arquivos da seção 4.3.

### 4.5 Mob e personagem (o mais complexo)

1. Blockbench: **File → New → Modded Entity**. Se quiser animações mais ricas, instale o plugin **GeckoLib**
   (File → Plugins → pesquise "GeckoLib") e use o formato de modelo animado do GeckoLib.
2. **Monte o corpo com cubos em grupos (ossos)**: cabeça, corpo, braços, pernas, cauda. Coloque o **pivot** de
   cada grupo na articulação (ombro, quadril, pescoço). Pivot errado faz o braço girar pelo meio.
3. **Textura**: com tudo modelado, use **Create Texture** com "Box UV" para o Blockbench organizar o mapa.
   Pinte no modelo 3D ou no editor 2D.
4. **Animações** (aba Animate), com estes nomes:
   - `idle` (parado, respirando);
   - `walk` (andar, em loop);
   - `attack` (golpe);
   - `death` (opcional);
   - específicas do mob (`cast` para magia, `roar`...).
5. Me mande o **`.bbmodel`**, que contém tudo.

**Você decide e me diz**: nome, vida e dano base (o mod escala com o level), velocidade, comportamento (agressivo,
neutro, foge), onde nasce (bioma, dimensão, à noite, em estruturas), drops e sons.

**Eu faço**: a conversão para código (modelo Java ou GeckoLib), o renderer, a IA, os atributos, o spawn, o level
escalado, os drops, os sons e o ovo de spawn.

### 4.6 Textura animada (minério brilhando, lava de cor diferente, item mágico)

1. Desenhe os quadros um abaixo do outro: 4 quadros de 16×16 resultam num PNG de 16×64.
2. Me diga a velocidade. Eu crio o arquivo `nome.png.mcmeta`, por exemplo:
   ```json
   { "animation": { "frametime": 3 } }
   ```
   `frametime` é quantos ticks cada quadro dura (20 ticks = 1 segundo).

### 4.7 Armadura

1. **Ícones** dos 4 itens (elmo, peitoral, calças, botas), cada um com 16×16.
2. **Textura no corpo**: duas imagens 64×32.
   - `<nome>_layer_1.png`: elmo, peitoral e botas;
   - `<nome>_layer_2.png`: calças.

   Abra uma armadura vanilla como **referência do layout** (veja a seção 5.2) e pinte a sua por cima de um arquivo
   vazio do mesmo tamanho.
3. No Blockbench dá para testar: crie um modelo "Player" e aplique a textura para ver no corpo.

**Você decide**: proteção, durabilidade, se é **pesada ou leve** (conta para as skills Armadura Pesada ou Leve) e o
material de reparo.

### 4.8 Sons

1. Grave ou gere o efeito (ChipTone para efeitos curtos, Audacity para gravações).
2. No Audacity: corte o silêncio do começo, normalize (**Efeito → Normalizar**, -1 dB), converta para mono
   (**Faixas → Mix → Mixar estéreo para mono**) e **Arquivo → Exportar → OGG**.
3. Faça **2 ou 3 variações** do mesmo som; o jogo sorteia entre elas e fica menos repetitivo.

---

## 5. Testar sua arte no jogo sem programar

### 5.1 Pacote de recursos de teste (troca ao vivo)

Dá para trocar as texturas do mod e ver o resultado **na hora**, sem me pedir build:

1. Em `.minecraft/resourcepacks/`, crie a pasta `MinhaArte`.
2. Dentro dela, crie o arquivo `pack.mcmeta`:
   ```json
   { "pack": { "pack_format": 34, "description": "Minha arte do Leveled World" } }
   ```
   (34 é o formato de pacote de recursos do 1.21.1.)
3. Coloque suas imagens no **mesmo caminho** das do mod. Exemplos com o que já existe:
   - `MinhaArte/assets/leveledworld/textures/item/skill_tome.png` (tomo de skill);
   - `MinhaArte/assets/leveledworld/textures/item/training_lesson.png` (lição de treinador);
   - `MinhaArte/assets/leveledworld/textures/mob_effect/bleeding.png` (ícone de Sangramento);
   - `MinhaArte/assets/leveledworld/textures/gui/sky/<skill>.png` (fundos das constelações).
4. No jogo: **Opções → Pacotes de recursos**, ative "MinhaArte".
5. Edite a imagem, salve e aperte **F3 + T** no jogo: as texturas recarregam na hora.

Assim você ajusta cor, contraste e legibilidade vendo no jogo de verdade, em vários fundos e iluminações.

### 5.2 Ver as texturas vanilla como referência

O `.jar` do Minecraft é um zip: copie `.minecraft/versions/1.21.1/1.21.1.jar`, renomeie a cópia para `.zip`, abra e
veja `assets/minecraft/textures/`. Use **só como referência** de tamanho, layout e estilo. As texturas vanilla são
da Mojang e não podem ir para o mod.

---

## 6. Dando a sua cara ao mod

- **Paleta própria**: escolha de 16 a 32 cores (pode partir de uma da Lospec e ajustar) e use **as mesmas cores no
  mod inteiro**. É o que mais deixa um mod com identidade.
- **Hue shifting**: em vez de só escurecer, puxe as sombras para azul ou roxo e as luzes para amarelo ou laranja.
  Fica muito mais vivo que preto e branco.
- **Silhueta primeiro**: se não dá para reconhecer o item só pela forma, nenhum detalhe salva.
- **Pouco ruído**: em 16×16, cada pixel conta. Prefira áreas limpas a pixels soltos.
- **Contraste e leitura**: teste o item no inventário, no chão e na mão, de dia e à noite.
- **Coerência com o que já existe**: o mod usa dourado envelhecido + azul-noite + espaço/constelações. Você pode
  manter essa linha ("fantasia celeste") ou propor outra; se mudar, eu atualizo a interface para combinar.

---

## 7. Como me entregar a arte

**Opção A: pelo GitHub (recomendada para muitos arquivos)**
1. Instale o **GitHub Desktop**, faça login e clone o repositório `leveledworld`.
2. Coloque os arquivos na pasta **`arte/`**, organizados por tipo: `arte/itens/`, `arte/blocos/`, `arte/mobs/`,
   `arte/sons/`.
3. Commit + Push, e me avise: "subi a arte do rubi em arte/".

**Opção B: pelo chat**
- Envie as imagens **no começo de uma mensagem**. Imagens enviadas enquanto estou no meio de uma tarefa podem
  não chegar.
- Arquivos `.bbmodel` e `.ogg` funcionam melhor pela opção A.

**Junto com a arte, me diga** (pode ser em texto livre):
- nome no jogo (PT e EN, se quiser);
- o que faz (status, efeito, receita, onde aparece);
- raridade e em que level de jogo ele deveria aparecer;
- para blocos: dureza, ferramenta e geração;
- para mobs: o que está na seção 4.5.

---

## 8. Roteiro de aprendizado sugerido

| Etapa | Foco | Exercício |
|---|---|---|
| 1 | Pixel art básica | 5 itens 16×16: gema, lingote, maçã, poção, pergaminho. Troque o tomo de skill pelo seu via pacote de recursos. |
| 2 | Blocos tileáveis | 1 pedra própria + 1 minério (pedra e ardósia) + o item que ele dropa. |
| 3 | Blockbench estático | Um altar ou bloco decorativo; depois uma arma 3D com a aba Display ajustada. |
| 4 | Mob simples | Um mob de 4 patas com `idle` e `walk`. |
| 5 | Mob completo | Um inimigo humanoide com ataque e textura detalhada. |
| 6 | Sons | Um som de golpe e um de coleta em 3 variações cada. |

**Onde aprender** (gratuito):
- **Saint11 (Pedro Medeiros)**, brasileiro, artista de Celeste: tutoriais curtos e visuais de pixel art (saint11.art).
- **Lospec**: tutoriais e paletas.
- No YouTube: **MortMort**, **Brandon James Greer** e **AdamCYounis** (pixel art); procure "Blockbench tutorial" e
  "Blockbench animation" para modelagem e animação.
- **Wiki do Blockbench** (blockbench.net/wiki): guias oficiais de cada formato.

---

## 9. Resumo rápido

1. Instale **Blockbench** (grátis) e um editor de pixel art (**Aseprite**, ou **Pixelorama**/**LibreSprite** grátis).
2. 16×16, PNG, nomes em minúsculas com `_`, luz do canto superior esquerdo, paleta própria.
3. Teste ao vivo com um pacote de recursos + **F3 + T**.
4. Itens e blocos: PNG. Modelos e mobs: `.bbmodel`. Sons: `.ogg` mono.
5. Mande para `arte/` no GitHub ou no começo de uma mensagem, dizendo o que cada coisa faz. Eu cuido do código.
