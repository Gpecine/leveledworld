# Plano: Pedras Guardiãs, Raças e Missões

Três sistemas novos, entregues em três versões: **0.9.0 Pedras Guardiãs**, **0.9.1 Raças** e **0.10.0 Missões**.
Todos se ligam ao que já existe (skills, perks, level dos mobs, tomos, loot) e são configuráveis. A arte (blocos,
ícones) entra com **arte provisória minha**, que você troca pela sua quando quiser (veja o tutorial de arte).

---

## 1. Pedras Guardiãs (0.9.0)

Pedras rúnicas espalhadas pelo mundo. Ao clicar com o botão direito, você recebe a **bênção** daquela pedra.
Só uma bênção fica ativa por vez; para trocar, basta ativar outra pedra.

### 1.1 As pedras

| Pedra | Tipo | Efeito |
|---|---|---|
| **Guerreiro** | Passivo | +20% de XP em Uma Mão, Duas Mãos, Arquearia, Bloqueio e Armadura Pesada |
| **Ladrão** | Passivo | +20% de XP em Armadura Leve, Furtividade e Atletismo |
| **Artesão** | Passivo | +20% de XP em Mineração, Lenhador, Ferraria e Agricultura |
| **Amante** | Passivo | +15% de XP em **todas** as skills |
| **Senhor** | Passivo | +3 de armadura e −15% de dano de explosões, fogo e veneno |
| **Corcel** | Passivo | +8% de velocidade e pular não gasta stamina |
| **Sombra** | Poder (1×/dia) | Invisibilidade total por 30 s; os mobs perdem o alvo |
| **Serpente** | Poder (1×/dia) | Paralisa (lentidão máxima) e envenena o alvo por 5 s |
| **Ritual** | Poder (1×/dia) | Cura total e remove todos os efeitos negativos |

- "1×/dia" = uma vez por **dia do Minecraft** (20 min reais).
- Os poderes são usados com uma tecla nova, **"Usar poder"** (padrão `V`, configurável nos controles).
- **Mago** fica reservada para o sistema de magias, junto com as pedras ligadas à Mana.

### 1.2 Onde aparecem

- **As três Guardiãs** (Guerreiro, Ladrão, Artesão) ficam **juntas, perto do spawn do mundo**, como no Skyrim:
  um círculo de pedra com as três runas, a no máximo ~150 blocos do spawn.
- **As outras seis** surgem pela Superfície, raras: cerca de 1 a cada 60×60 chunks. Cada uma vem num pequeno
  círculo de pedra com a runa no centro.
- Em mundos já existentes, aparecem nos chunks novos. As Guardiãs são criadas no primeiro login depois da
  atualização, se ainda não houver nenhuma.

### 1.3 Interface

- Na tela **K**, um selo mostra a bênção ativa (ícone + nome), com a descrição no tooltip.
- No HUD, ao lado do painel, o ícone do poder fica com uma **barra de recarga** enquanto não pode ser usado.
- Ativar uma pedra toca um som e mostra uma animação de partículas subindo da runa.

### 1.4 Arte (provisória até você fazer a sua)

- 9 texturas de bloco 16×16, uma runa por pedra, mais a pedra-base;
- 9 ícones 16×16 de bênção para o HUD e a tela K.

---

## 2. Raças (0.9.1)

No primeiro login, uma tela pede para escolher a raça. A raça dá **bônus iniciais em skills**, uma
**habilidade passiva** e um **poder 1×/dia**, e muda levemente o **tamanho** do personagem (atributo de escala do
1.21.1). Os nomes são próprios do mod, sem usar os do Skyrim.

| Raça | Skills iniciais | Passiva | Poder (1×/dia) | Tamanho |
|---|---|---|---|---|
| **Humano** | +5 em todas | +10% de XP em todas as skills | **Determinação**: XP de skill em dobro por 60 s | 100% |
| **Anão** | +10 Mineração, +10 Ferraria | 50% de resistência a fogo; visão no escuro em cavernas | **Pele de Pedra**: +8 de armadura por 60 s | 85% |
| **Elfo** | +10 Arquearia, +10 Furtividade | 50% de resistência a veneno; +10% de velocidade em florestas | **Olhos da Floresta**: mobs num raio de 32 blocos brilham por 30 s | 100% |
| **Orc** | +10 Duas Mãos, +10 Armadura Pesada | +15% de vida máxima | **Fúria**: +50% de dano corpo a corpo por 30 s | 108% |
| **Nórdico do Gelo** | +10 Uma Mão, +10 Bloqueio | Imune a congelamento (neve fofa); 50% de resistência a frio | **Grito de Guerra**: mobs num raio de 10 blocos fogem por 10 s | 105% |
| **Felino** | +10 Furtividade, +10 Atletismo | Ataques desarmados +4 de dano; sem dano de queda até 6 blocos | **Visão Noturna**: liga e desliga, sem recarga | 95% |
| **Anfíbio** | +10 Atletismo, +10 Agricultura | Respira embaixo d'água; nada 30% mais rápido | **Regeneração**: Regeneração II por 20 s | 100% |

- **Mundos existentes**: na primeira entrada depois da atualização, a tela de raça aparece e os bônus são somados
  aos levels atuais.
- **Trocar de raça**: uma vez, de graça, nos primeiros 10 minutos de jogo. Depois, só com a **Poção da Renovação**
  (rara, de loot de chefes e baús de fortalezas) ou com o comando de admin.
- Os poderes usam a mesma tecla **"Usar poder"**. Com poder de raça **e** de pedra, um segundo toque rápido
  (ou `Shift + V`) alterna entre eles, e o HUD mostra qual está selecionado.

### Arte (provisória)

- 7 ícones de raça (32×32) para a tela de escolha;
- 7 ícones de poder (16×16).

---

## 3. Missões: Quadro de Contratos (0.10.0)

Cada vila ganha um **Quadro de Contratos**. Ele oferece de 3 a 5 contratos que **renovam a cada dia** do Minecraft.
Você aceita até **3 contratos ativos**, acompanha o progresso e volta ao quadro (de qualquer vila) para receber a
recompensa.

### 3.1 Tipos de contrato

| Tipo | Exemplo | Como cumprir |
|---|---|---|
| **Caça** | "Elimine 12 zumbis" | Matar mobs de um tipo (escolhido entre os que existem na região) |
| **Procurado** | "Procurado: Gorvak, o Esqueleto Ancião" | Um **mob elite com nome** surge num ponto a 300–800 blocos; as coordenadas e a direção aparecem no diário. Ele tem +5 levels, mais vida e um drop garantido |
| **Coleta** | "Entregue 32 carvões e 16 ferros brutos" | Entregar os itens no quadro |
| **Colheita** | "Entregue 64 trigos e 32 cenouras" | Entregar itens de Agricultura |
| **Lenha** | "Entregue 48 troncos de carvalho" | Entregar troncos |
| **Exploração** | "Encontre um templo do deserto" | Chegar a uma estrutura; a direção aparece no diário |

### 3.2 Escala com o level

- As quantidades, o level dos mobs Procurados e as recompensas **sobem com o seu level** (um contrato de level 40
  pede mais e paga bem mais que um de level 5).
- Os contratos ficam em três dificuldades, marcadas no quadro: **Fácil**, **Normal** e **Difícil**. Os difíceis pagam
  o dobro.

### 3.3 Recompensas

- Esmeraldas (escala com o level e a dificuldade);
- **XP de skill** ligado ao contrato: Caça e Procurado dão Uma Mão, Duas Mãos ou Arquearia conforme a arma usada;
  Coleta dá Mineração; Colheita dá Agricultura; Lenha dá Lenhador; Exploração dá Atletismo;
- chance de **tomo de skill** e de itens das tabelas de loot bônus;
- Procurado: drop garantido + chance de **Poção da Renovação**.

### 3.4 Interface

- **Tela do quadro**: os contratos do dia como "cartazes", com dificuldade, objetivo, prazo e recompensa;
  botão Aceitar.
- **Diário (tecla `J`)**: contratos ativos, progresso (8/12), direção e distância do alvo, e botão para abandonar.
- **HUD**: um rastreador discreto, abaixo do painel, com o contrato selecionado ("Zumbis: 8/12").
- Mensagem e som ao concluir; os de entrega se concluem ao clicar no quadro com os itens no inventário.

### 3.5 Onde o quadro aparece

- É adicionado às **vilas geradas** (perto do sino) em todos os tipos de vila.
- Também é **fabricável** (tábuas + papel + placa de madeira), para colocar no seu povoado.

### Arte (provisória)

- Textura do Quadro de Contratos (bloco);
- fundo de pergaminho da tela de contratos;
- 6 ícones de tipo de contrato (16×16).

---

## 4. Ordem e esforço

| Versão | Conteúdo | Depende de |
|---|---|---|
| **0.9.0** | Pedras Guardiãs: bloco, geração no mundo, bênçãos, 3 poderes, tecla "Usar poder", HUD de recarga | — |
| **0.9.1** | Raças: tela de escolha, bônus, passivas, 7 poderes, escala, Poção da Renovação | Tecla de poder da 0.9.0 |
| **0.10.0** | Missões: Quadro de Contratos, 6 tipos, mobs Procurados, diário `J`, rastreador no HUD, geração nas vilas | Poção da Renovação da 0.9.1 (recompensa) |

Cada versão vai para `releases/` no GitHub, testada no cliente virtual antes da entrega.

## 5. Configuração

Tudo em `leveledworld-server.toml`: bônus de cada pedra, frequência das pedras no mundo, recarga dos poderes,
bônus e tamanho de cada raça, liberar ou não a troca de raça, quantos contratos por dia e ativos, e o multiplicador
de recompensas.

## 6. Perguntas em aberto

1. As **7 raças** e os bônus da tabela estão bons, ou quer mudar alguma (nome, bônus, poder, tamanho)?
2. O tamanho diferente por raça (Anão menor, Orc maior) fica? Ele muda a hitbox e o alcance, não só a aparência.
   Exemplo: o Orc a 108% passa a ter 1,94 bloco de altura e precisa de 2 blocos livres para andar.
3. Os contratos devem ter **prazo** (ex.: 3 dias do Minecraft para cumprir) ou ficar abertos até você abandonar?
