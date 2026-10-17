# Plano — Sistema de Skills 2.0

> Documento de planejamento. Nada aqui está implementado ainda. Números são propostas iniciais para balancear em jogo.

## 1. Visão geral

| Hoje (0.7) | Depois |
|---|---|
| 7 skills | **12 skills** em 3 grupos (Guerreiro, Ladrão, Artesão) |
| 4 perks por skill, automáticos | **15 perks por skill** (~180), em árvores com ramos e ranks |
| Sem escolha | **1 ponto de perk por level** do personagem — você monta a build |
| Skill para no 100 | **Skills lendárias**: volta ao 15, devolve os perks, progresso infinito |
| Só treina usando | **Livros de skill** no loot e **treinadores** (aldeões) |
| XP simples | **XP balanceado** por dificuldade do alvo, raridade do recurso e anti-farm |

## 2. As 12 skills

| Grupo | Skill | Treina com | Fundo da constelação |
|---|---|---|---|
| Guerreiro | **Uma Mão** | espadas | pilares (atual de Combate) |
| Guerreiro | **Duas Mãos** *(nova)* | machados, maça, tridente corpo a corpo | *imagem nova* |
| Guerreiro | **Arquearia** | arco, besta, tridente arremessado | galáxia espiral |
| Guerreiro | **Bloqueio** | escudo | nebulosa azul |
| Guerreiro | **Armadura Pesada** *(atual Armadura)* | ferro, diamante, netherite | nebulosa vermelha |
| Ladrão | **Armadura Leve** *(nova)* | couro, cota de malha, casco de tartaruga | *imagem nova* |
| Ladrão | **Furtividade** *(nova)* | agachado perto de inimigos sem ser visto, ataques furtivos | *imagem nova* |
| Ladrão | **Atletismo** *(nova)* | correr, nadar, pular, escalar, sobreviver a quedas | *imagem nova* |
| Artesão | **Mineração** | picareta (minérios valem mais) | asteroides dourados |
| Artesão | **Lenhador** | machado em troncos | olho verde |
| Artesão | **Ferraria** | fabricar e reparar equipamento | supernova |
| Artesão | **Agricultura** *(nova)* | colher plantações maduras, criar animais, pescar | *imagem nova* |

São necessárias **5 imagens novas de fundo** (Duas Mãos, Armadura Leve, Furtividade, Atletismo, Agricultura), no mesmo estilo das atuais (1672×941 ou maior).

**Migração de mundos antigos:** o level de "Armadura" vira o level de Armadura Pesada **e** Leve; os perks automáticos antigos somem e o jogador recebe pontos de perk equivalentes ao seu level para redistribuir.

## 3. Pontos de perk

- **1 ponto de perk por level** do personagem (separado dos pontos de atributo Vida/Stamina/Mana).
- Cada perk exige: **nível mínimo da skill** + **perk anterior** do ramo (linha da constelação).
- Perks com **ranks** (ex.: 1/3) custam 1 ponto por rank, cada rank com nível mínimo próprio.
- Comprar: clique na estrela → janela com nome, efeito, requisitos e botão **Aprender**.
- Level 100 ≈ 99 pontos para ~200 compras possíveis → é preciso escolher. Skills lendárias e livros dão mais folga.
- Comando de admin para resetar perks (e, opcional, um item raro "Pergaminho do Esquecimento" para respec).

## 4. Árvores de perks

Formato: **Perk** (nível mínimo · ranks) — efeito. `←` = requer o perk indicado.

### 4.1 Uma Mão (espadas)
1. **Golpe Firme** (0/20/40 · 3) — +10% / 20% / 30% de dano com espadas
2. **Lâmina Afiada** (20) ← 1 — 15% de chance de sangramento (dano por 3s)
3. **Corte Profundo** (40) ← 2 — sangramento mais longo e acumula até 2×
4. **Dança das Lâminas** (60) ← 3 — golpes seguidos no mesmo alvo +5% cada (máx. +25%)
5. **Olho do Duelista** (20) ← 1 — +8% de chance de crítico (×1,5)
6. **Golpe Preciso** (40) ← 5 — crítico causa ×1,75
7. **Executor** (70) ← 6 — +40% de dano em alvos com menos de 25% de vida
8. **Passo Leve** (25) ← 1 — ataques gastam 30% menos stamina
9. **Investida** (45) ← 8 — atacar correndo: +30% de dano e empurrão
10. **Contra-ataque** (55) ← 8 — após bloquear com escudo, o próximo golpe em 2s causa +50%
11. **Fúria do Guerreiro** (50) ← 1 — abaixo de 30% de vida: +20% de dano
12. **Lâmina Envenenada** (65) ← 2 — 10% de chance de envenenar
13. **Desarmar** (75) ← 7 — 5% de chance de mobs com arma a derrubarem
14. **Sede de Sangue** (85) ← 4 — matar um inimigo cura 1 coração
15. **Mestre das Lâminas** (100) ← 4, 7 ou 9 — +10% de velocidade de ataque e 10% de golpe duplo

### 4.2 Duas Mãos (machados, maça, tridente) — nova
1. **Bárbaro** (0/20/40 · 3) — +10% / 20% / 30% de dano
2. **Rachador** (20) ← 1 — golpes ignoram 25% da armadura
3. **Esmagador** (45) ← 2 — ignoram 50% da armadura
4. **Golpe Pesado** (25) ← 1 — ataque com carga cheia empurra o dobro
5. **Terremoto** (60) ← 4 — ataque com carga cheia atinge inimigos ao redor (50% do dano)
6. **Talho Selvagem** (20) ← 1 — 15% de chance de sangramento
7. **Hemorragia** (50) ← 6 — sangramento mais forte
8. **Ceifador** (70) ← 7 — matar um inimigo dá Força I por 3s
9. **Fôlego de Guerra** (30) ← 1 — ataques gastam 30% menos stamina
10. **Implacável** (55) ← 9 — cada acerto recupera 3 de stamina
11. **Crítico Brutal** (45) ← 1 — +8% de chance de crítico
12. **Golpe Devastador** (75) ← 11 — crítico causa ×2
13. **Pele de Ferro** (65) ← 4 — segurando arma de duas mãos: +2 de armadura
14. **Lanceiro** (40) ← 1 — tridente arremessado +25% de dano
15. **Senhor da Guerra** (100) ← 3, 5 ou 12 — +15% de dano e resistência a empurrão ao atacar

### 4.3 Arquearia
1. **Olho de Águia** (0/20/40 · 3) — +10% / 20% / 30% de dano à distância
2. **Mão Rápida** (25) ← 1 — puxa o arco 20% mais rápido
3. **Disparo Duplo** (65) ← 2 — 15% de chance de disparar uma segunda flecha
4. **Chuva de Flechas** (85) ← 3 — agachado: dispara 3 flechas em leque (gasta 3)
5. **Recuperação** (30) ← 1 — 33% de chance de recuperar a flecha
6. **Aljava Generosa** (60) ← 5 — 50% de chance
7. **Caçador** (20) ← 1 — +25% de dano em animais e mobs voadores
8. **Tiro na Cabeça** (45) ← 1 — acerto na cabeça causa +50%
9. **Flecha Paralisante** (55) ← 8 — 10% de chance de lentidão forte por 2s
10. **Perfurante** (70) ← 8 — flechas atravessam 1 inimigo
11. **Besteiro** (20) ← 1 — bestas +20% de dano e recarga 15% mais rápida
12. **Vento a Favor** (40) ← 1 — flechas 25% mais rápidas (mais alcance)
13. **Tiro Furtivo** (50) ← 1 — tiros não detectados causam ×2
14. **Olho do Falcão** (75) ← 12 — +20% de dano em alvos a mais de 20 blocos
15. **Mestre Arqueiro** (100) ← 4, 6 ou 14 — +15% de dano e 20% de chance de crítico

### 4.4 Bloqueio
1. **Guardião** (0/30 · 2) — bloquear recupera 3 / 6 de stamina
2. **Retaliação** (25) ← 1 — devolve 25% do dano bloqueado
3. **Espinhos de Aço** (60) ← 2 — devolve 50%
4. **Empurrão de Escudo** (25) ← 1 — bloquear empurra o atacante
5. **Golpe de Escudo** (50) ← 4 — bloquear atordoa (lentidão + fraqueza por 2s)
6. **Defletor** (45) ← 1 — flechas bloqueadas voltam para o atirador
7. **Guarda Elemental** (55) ← 6 — escudo erguido: −30% de dano de fogo e explosões
8. **Contra-golpe** (35) ← 1 — após bloquear, o próximo golpe +30%
9. **Recuperação Rápida** (30) ← 1 — escudo desativado por machado volta 50% mais rápido
10. **Muralha Inabalável** (40) ← 9 — imune a empurrão ao bloquear
11. **Escudo Reforçado** (45) ← 1 — escudos gastam 50% menos durabilidade
12. **Bastião** (65) ← 11 — segurando escudo (mesmo abaixado): +2 de armadura
13. **Disciplina** (50) ← 1 — bloquear dá Resistência I por 2s
14. **Último Recurso** (80) ← 13 — abaixo de 25% de vida, bloquear cura 1 coração (recarga 10s)
15. **Fortaleza Imortal** (100) ← 3, 5 ou 14 — 15% de chance de bloquear golpes mesmo sem erguer o escudo

### 4.5 Armadura Pesada (ferro, diamante, netherite)
1. **Juggernaut** (0/20/40 · 3) — +10% / 20% / 30% de armadura das peças pesadas
2. **Conjunto Completo** (30) ← 1 — 4 peças pesadas: +10% de armadura
3. **Tanque** (50) ← 2 — +30% de resistência a empurrão
4. **Pele de Rocha** (70) ← 3 — conjunto completo: −10% de todo dano
5. **Couraça** (20) ← 1 — −15% de dano de projéteis
6. **Muralha de Ferro** (35) ← 5 — −20% de dano de explosões
7. **Inquebrável** (30) ← 1 — armadura pesada gasta 30% menos durabilidade
8. **Forja Viva** (65) ← 7 — armadura pesada se repara lentamente
9. **Resiliência** (40) ← 1 — veneno, wither e lentidão duram 30% menos
10. **Contra-impacto** (45) ← 1 — −25% de dano de queda
11. **Refletir** (60) ← 4 — 10% de chance de devolver 50% do dano corpo a corpo
12. **Peito de Aço** (55) ← 2 — críticos recebidos não causam dano extra
13. **Imparável** (75) ← 3 — imune a empurrão de explosões
14. **Rocha Viva** (85) ← 11 — abaixo de 30% de vida: +20% de armadura
15. **Colosso** (100) ← 4, 8 ou 14 — conjunto completo: +2 corações e imunidade a empurrão

### 4.6 Armadura Leve (couro, cota de malha, tartaruga) — nova
1. **Agilidade** (0/20/40 · 3) — +10% / 20% / 30% de armadura das peças leves
2. **Esquiva** (25) ← 1 — 5% de chance de esquivar (anular) ataques
3. **Reflexos** (50) ← 2 — 10% de chance
4. **Sombra Veloz** (75) ← 3 — 15% de chance
5. **Passos Leves** (20) ← 1 — 2+ peças leves: +5% de velocidade de movimento
6. **Vento** (45) ← 5 — +10% de velocidade
7. **Pés Ágeis** (30) ← 1 — −30% de dano de queda
8. **Conjunto Completo** (35) ← 1 — 4 peças leves: +15% de armadura
9. **Fôlego** (40) ← 1 — stamina regenera 25% mais rápido usando armadura leve
10. **Mergulhador** (35) ← 1 — +50% de tempo de oxigênio
11. **Rolamento** (55) ← 7 — cair agachado: −50% de dano de queda
12. **Esquiva de Projéteis** (60) ← 2 — 15% de chance de esquivar flechas
13. **Leve como Pena** (65) ← 6 — conjunto completo: pulo mais alto
14. **Vento Cortante** (80) ← 4 — depois de esquivar, o próximo ataque +50%
15. **Mestre da Esquiva** (100) ← 4, 13 ou 14 — +10% de esquiva e esquivar recupera 10 de stamina

### 4.7 Furtividade — nova
**Mecânica base:** agachado, os mobs demoram mais para te notar (alcance de detecção menor). Atacar um mob que não está mirando em você é um **ataque furtivo** (×2 corpo a corpo, ×1,5 à distância). Um ícone de olho no HUD mostra se você está oculto / sendo notado / detectado.
1. **Furtivo** (0/20/40 · 3) — mobs te detectam a −15% / 25% / 35% da distância
2. **Ataque Furtivo** (20) ← 1 — ataques furtivos corpo a corpo ×2,5
3. **Assassino** (50) ← 2 — ataques furtivos com espada ×3,5
4. **Golpe Mortal** (80) ← 3 — ataques furtivos com espada ×6
5. **Arqueiro Oculto** (30) ← 1 — tiros furtivos ×2
6. **Passos Silenciosos** (25) ← 1 — agachado, não dispara sensores sculk nem o Warden
7. **Sombra** (40) ← 6 — andar agachado 30% mais rápido
8. **Desaparecer** (60) ← 7 — parado agachado por 3s: invisível até se mover
9. **Ladrão** (35) ← 1 — inimigos mortos furtivamente: +30% de chance de drop extra
10. **Evasão** (45) ← 1 — ao ser detectado, o mob hesita 1s antes de atacar
11. **Sem Rastro** (55) ← 6 — Endermen não ficam agressivos quando você olha para eles
12. **Veneno Furtivo** (65) ← 2 — ataques furtivos envenenam
13. **Fuga** (50) ← 10 — agachado: −20% de dano de projéteis
14. **Mestre das Sombras** (85) ← 8 — invisível ao se agachar em lugares escuros
15. **Fantasma** (100) ← 4, 11 ou 14 — agachar e se afastar 8+ blocos faz os mobs perderem o alvo (recarga 30s)

### 4.8 Atletismo — nova
**XP:** distância correndo e nadando, pulos, escaladas e quedas sobrevividas (com limite por minuto, contra farm).
1. **Fôlego** (0/20/40 · 3) — stamina máxima +10 / 20 / 30
2. **Corredor** (20) ← 1 — correr gasta 20% menos stamina
3. **Maratonista** (50) ← 2 — 40% menos
4. **Velocista** (35) ← 1 — +5% de velocidade de corrida
5. **Relâmpago** (70) ← 4 — +10% de velocidade de corrida
6. **Saltador** (25) ← 1 — pular gasta 50% menos stamina
7. **Pernas de Mola** (45) ← 6 — pulo mais alto (+0,5 bloco)
8. **Queda Suave** (30) ← 1 — −25% de dano de queda
9. **Gato** (60) ← 8 — −50% e quedas de até 6 blocos não machucam
10. **Nadador** (20) ← 1 — +20% de velocidade nadando
11. **Peixe** (50) ← 10 — +50% de oxigênio e nadar não gasta stamina
12. **Escalador** (40) ← 1 — sobe escadas e trepadeiras 30% mais rápido
13. **Recuperação** (55) ← 1 — stamina regenera 25% mais rápido
14. **Segundo Fôlego** (75) ← 13 — ao zerar a stamina, recupera 50% na hora (recarga 60s)
15. **Incansável** (100) ← 3, 5 ou 14 — correr não gasta fome e a stamina regenera correndo

### 4.9 Mineração
1. **Braço Forte** (0/20/40 · 3) — +15% / 30% / 45% de velocidade com picareta
2. **Prospector** (20) ← 1 — 5% de chance de minério dobrado
3. **Veio Rico** (50) ← 2 — 12% de chance
4. **Coração da Montanha** (80) ← 3 — 20% de chance
5. **Faro de Minério** (30) ← 1 — minerar um minério revela minérios iguais num raio de 4 blocos (brilho por 5s)
6. **Visão Profunda** (60) ← 5 — raio de 8 blocos, inclui diamante e ancient debris
7. **Escavador** (25) ← 1 — pedra e deepslate 25% mais rápido
8. **Mineração em Veio** (55) ← 7 — agachado: minera o veio inteiro (até 12 blocos)
9. **Resistente** (35) ← 1 — picaretas gastam 25% menos durabilidade
10. **Fundidor** (45) ← 1 — 20% de chance de ferro, ouro e cobre saírem como lingote
11. **Geólogo** (40) ← 1 — +50% de XP vanilla dos minérios
12. **Pedras Preciosas** (65) ← 3 — 1% de chance de esmeralda ou diamante em pedra comum
13. **Morador das Cavernas** (30) ← 1 — abaixo de y=0: visão no escuro
14. **Incansável** (50) ← 9 — minerar não gasta fome
15. **Mestre Minerador** (100) ← 4, 6 ou 8 — +1 Eficiência e +1 Fortuna efetivas em minérios

### 4.10 Lenhador
1. **Machado Afiado** (0/20/40 · 3) — +20% / 40% / 60% de velocidade em troncos
2. **Colheita Farta** (25) ← 1 — 15% de chance de tronco extra
3. **Floresta Generosa** (60) ← 2 — 30% de chance
4. **Derrubada** (40) ← 1 — agachado: derruba a árvore inteira
5. **Silvicultor** (55) ← 4 — replanta a muda automaticamente ao derrubar
6. **Mudas** (20) ← 1 — folhas dão +50% de mudas e maçãs
7. **Pomar Dourado** (50) ← 6 — 0,5% de chance de maçã dourada nas folhas
8. **Durabilidade** (30) ← 1 — machados gastam 25% menos
9. **Carpinteiro** (35) ← 1 — cada tronco vira 5 tábuas
10. **Golpe do Lenhador** (45) ← 1 — machados como arma: +10% de dano
11. **Ritmo** (40) ← 1 — cortar troncos dá +10% de velocidade de movimento por 5s
12. **Coletor** (50) ← 2 — caules do Nether e cogumelos gigantes contam como troncos
13. **Seiva** (65) ← 6 — comer maçã cura 2 corações extras
14. **Espírito da Floresta** (80) ← 5 — derrubar árvores dá o dobro de XP
15. **Guardião da Floresta** (100) ← 3, 5 ou 14 — derrubar árvores não gasta durabilidade

### 4.11 Ferraria
**Novidade:** itens fabricados ganham **qualidade** (Comum / Fina / Excelente / Obra-prima) com bônus de atributos, e a assinatura "Forjado por <nome>".
1. **Aprendiz** (0/20/40 · 3) — equipamento fabricado com +10% / 20% / 30% de durabilidade
2. **Ferreiro do Ferro** (20) ← 1 — itens de ferro fabricados com qualidade melhor
3. **Ferreiro do Diamante** (50) ← 2 — o mesmo para diamante
4. **Ferreiro de Netherite** (80) ← 3 — o mesmo para netherite (upgrade na mesa de ferraria)
5. **Forja Arcana** (30) ← 1 — 10% de chance do item sair encantado
6. **Encantador** (55) ← 5 — 20% de chance, com encantamentos mais fortes
7. **Forja Lendária** (85) ← 6 — 30% de chance, com encantamentos de nível 30
8. **Reparador** (25) ← 1 — reparar na bigorna custa 50% menos XP
9. **Mestre da Bigorna** (60) ← 8 — a bigorna nunca fica "caro demais"
10. **Economia** (35) ← 1 — 15% de chance de não gastar um material
11. **Afiação** (40) ← 2 — armas fabricadas +1 de dano
12. **Têmpera** (45) ← 2 — armaduras fabricadas +1 de armadura
13. **Reciclagem** (50) ← 10 — derreter equipamento na fornalha devolve mais pepitas
14. **Assinatura** (65) ← 11 ou 12 — qualidade mínima "Fina"
15. **Obra-prima** (100) ← 4, 7 ou 14 — 10% de chance de "Obra-prima" (+20% em todos os atributos, nome dourado)

### 4.12 Agricultura — nova
**XP:** colher plantações maduras, reproduzir animais, tosquiar, ordenhar e pescar.
1. **Mão Verde** (0/20/40 · 3) — +10% / 20% / 30% de chance de colheita extra
2. **Fertilizante** (20) ← 1 — farinha de osso: 50% de chance de não ser gasta
3. **Crescimento** (50) ← 2 — plantações perto de você crescem 20% mais rápido
4. **Mestre do Campo** (85) ← 3 — plantações num raio de 8 blocos crescem 2× mais rápido
5. **Replantio** (25) ← 1 — clique direito numa planta madura colhe e replanta
6. **Colheita em Área** (55) ← 5 — agachado: colhe 3×3
7. **Criador** (25) ← 1 — 25% de chance de gêmeos ao reproduzir
8. **Criador Experiente** (50) ← 7 — filhotes crescem 50% mais rápido
9. **Açougueiro** (40) ← 7 — animais dão +1 de carne
10. **Pastor** (30) ← 7 — tosquiar dá +1 de lã
11. **Cozinheiro** (30) ← 1 — comidas restauram 20% mais fome
12. **Banquete** (60) ← 11 — comer dá Regeneração I por 3s
13. **Pescador** (35) ← 1 — pesca 20% mais rápida e mais tesouros
14. **Safra Dourada** (75) ← 4 ou 6 — 2% de chance de cenoura dourada ou maçã dourada na colheita
15. **Senhor da Terra** (100) ← 4, 8 ou 14 — colheitas e animais dão o dobro de XP de skill e +1 item

## 5. Skills lendárias
- Ao chegar no **100**, aparece o botão **Tornar Lendária** na constelação.
- A skill volta ao **15**, os **pontos de perk** gastos nela voltam, e o perk fica marcado com uma estrela ★ por vez que foi lendária.
- O XP da skill continua alimentando o level do personagem → **progresso infinito**. O level máximo passa a ser configurável (padrão sugerido: 250).

## 6. Livros de skill e treinadores
**Livros:** item "Tomo de <Skill>" que dá **+1 nível** na skill ao ler (consumido).
- Fontes: loot bônus incomum/raro, baús de estruturas (masmorras, fortalezas, cidades do End), tesouro de pesca.
- Livros mais raros: "Tomo Antigo" dá +2 níveis.

**Treinadores:** aldeões vendem **lições** por esmeraldas.

| Profissão | Ensina |
|---|---|
| Armeiro (weaponsmith) | Uma Mão, Duas Mãos |
| Ferreiro de armaduras (armorer) | Armadura Pesada, Armadura Leve, Bloqueio |
| Flecheiro (fletcher) | Arquearia |
| Ferramenteiro (toolsmith) | Mineração, Lenhador, Ferraria |
| Fazendeiro / Açougueiro | Agricultura |
| Cartógrafo | Atletismo |
| Mercador ambulante | Furtividade |

- O nível do aldeão limita até onde ele ensina (Novato → 25, … Mestre → 75); acima de 75 só praticando.
- Limite de **5 lições por level do personagem** (como no Skyrim). O preço sobe com o nível da skill.

## 7. Balanceamento de XP
| Skill | XP por | Regras |
|---|---|---|
| Uma Mão / Duas Mãos / Arquearia | dano causado | × fator de dificuldade (level do mob ÷ seu level, entre 0,5 e 2) · bônus ao matar · mobs de spawner dão 25% |
| Bloqueio | dano bloqueado | limitado à vida do atacante |
| Armaduras | dano recebido de inimigos | dividido entre leve e pesada conforme as peças usadas |
| Furtividade | tempo agachado perto de inimigos sem ser visto + ataques furtivos | só conta com inimigo a até 16 blocos |
| Atletismo | blocos corridos e nadados, quedas sobrevividas | limite por minuto |
| Mineração | por minério: carvão 2 · cobre 2 · lápis 4 · ferro 4 · redstone 4 · ouro 6 · esmeralda 12 · diamante 15 · ancient debris 25 · pedra 0,3 | Toque de Seda e blocos colocados por jogador = 0 |
| Lenhador | por tronco | troncos colocados por jogador = 0 |
| Ferraria | por item fabricado, conforme o material (couro < ferro < diamante < netherite) | reparar também dá XP |
| Agricultura | colheita madura, reprodução, tosquia, pesca | plantas sem amadurecer = 0 |

- **Anti-farm:** o mod rastreia os blocos colocados por jogadores (troncos e minérios) para não darem XP. Mobs de spawner e de farms dão XP reduzido.
- Multiplicador de XP **por skill** na config.

## 8. Interface
- **Constelação = árvore de perks:** estrelas ligadas pelos requisitos. Estrelas acesas = aprendidas, brilhando = disponíveis, apagadas = travadas. Ranks aparecem como "2/3".
- Clique numa estrela → janela com nome, efeito, requisitos e **Aprender**. Botão **Tornar Lendária** no 100.
- **Contador de pontos de perk** no painel inferior, ao lado dos pontos de atributo.
- **12 skills no carrossel**, com o grupo (Guerreiro / Ladrão / Artesão) indicado.
- **Ícone de furtividade** no HUD (olho fechado / meio aberto / aberto).

## 9. Arquitetura técnica (resumo)
- **Perks em JSON** (`data/leveledworld/perks/<skill>.json`): id, posição da estrela, requisitos, ranks e efeitos. Dá para ajustar o balanceamento sem mexer no código e criar perks por datapack.
- **Biblioteca de efeitos** reutilizável (bônus de dano por tipo de arma, crítico, sangramento, chance de esquiva, redução de dano por tipo, velocidade, drops extras, custo de stamina…). Cada perk combina efeitos dessa biblioteca.
- Sincronização servidor → cliente das árvores, perks comprados e pontos.
- Rastreador de blocos colocados por jogadores, guardado nos chunks.

## 10. Fases de implementação
| Fase | Versão | Conteúdo |
|---|---|---|
| 1 | 0.8 | **Fundação:** perks em JSON, pontos de perk, compra na constelação, migração dos saves. Árvores completas das 7 skills atuais (Armadura → Pesada). |
| 2 | 0.9 | **Novas skills de combate:** Duas Mãos, Armadura Leve, Furtividade (detecção + HUD) |
| 3 | 0.10 | **Atletismo e Agricultura** |
| 4 | 0.11 | **Balanceamento de XP + anti-farm** |
| 5 | 0.12 | **Skills lendárias** + level máximo configurável |
| 6 | 0.13 | **Livros de skill e treinadores** |

Cada fase é testada no cliente virtual antes de ir para você, com jar em `releases/` e entrada no CHANGELOG.
