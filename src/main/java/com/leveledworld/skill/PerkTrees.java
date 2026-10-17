package com.leveledworld.skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.leveledworld.skill.Stat.*;

/** Todas as árvores de perks (docs/plano-skills.md). Comum a cliente e servidor. */
public final class PerkTrees {
    private static final Map<Skill, List<PerkDef>> TREES = new EnumMap<>(Skill.class);
    private static final Map<String, PerkDef> BY_ID = new HashMap<>();

    public static List<PerkDef> of(Skill skill) {
        return TREES.getOrDefault(skill, List.of());
    }

    public static PerkDef byId(String id) {
        return BY_ID.get(id);
    }

    public static PerkDef byLocal(Skill skill, String localId) {
        return BY_ID.get(skill.id() + "." + localId);
    }

    // ---------- DSL ----------

    private static int[] lv(int... levels) { return levels; }
    private static String[] req(String... ids) { return ids; }
    private static PerkDef.Effect st(Stat stat, double... values) { return new PerkDef.Effect(stat, values); }
    private static final String[] ROOT = new String[0];

    private static final class Builder {
        final Skill skill;
        final List<PerkDef> perks = new ArrayList<>();

        Builder(Skill skill) { this.skill = skill; }

        Builder perk(String id, int[] levels, String[] requires, PerkDef.Effect... effects) {
            perks.add(new PerkDef(skill, id, levels, List.of(requires), List.of(effects), perks.size()));
            return this;
        }

        void done() {
            layout(perks);
            TREES.put(skill, Collections.unmodifiableList(perks));
            for (PerkDef p : perks) BY_ID.put(p.id, p);
        }
    }

    private static Builder tree(Skill skill) { return new Builder(skill); }

    /** Layout em camadas: profundidade = maior caminho desde a raiz (embaixo); x pelo baricentro dos pais. */
    private static void layout(List<PerkDef> perks) {
        Map<String, PerkDef> local = new LinkedHashMap<>();
        for (PerkDef p : perks) local.put(p.localId, p);
        Map<PerkDef, Integer> depth = new HashMap<>();
        for (PerkDef p : perks) depthOf(p, local, depth);
        int maxDepth = depth.values().stream().max(Integer::compare).orElse(0);
        List<List<PerkDef>> layers = new ArrayList<>();
        for (int d = 0; d <= maxDepth; d++) layers.add(new ArrayList<>());
        for (PerkDef p : perks) layers.get(depth.get(p)).add(p);
        final float gap = 0.32f;
        for (int d = 0; d <= maxDepth; d++) {
            List<PerkDef> layer = layers.get(d);
            int n = layer.size();
            float[] desired = new float[n];
            if (d == 0) {
                for (int i = 0; i < n; i++) desired[i] = n == 1 ? 0 : -gap * (n - 1) / 2 + gap * i;
            } else {
                layer.sort((a, b) -> Float.compare(parentX(a, local), parentX(b, local)));
                for (int i = 0; i < n; i++) desired[i] = parentX(layer.get(i), local);
            }
            // Mantém a ordem, respeita o espaçamento mínimo e recentraliza em torno da média desejada.
            float[] x = desired.clone();
            for (int i = 1; i < n; i++) x[i] = Math.max(x[i], x[i - 1] + gap);
            float meanDesired = 0, meanX = 0;
            for (int i = 0; i < n; i++) { meanDesired += desired[i]; meanX += x[i]; }
            float shift = (meanDesired - meanX) / Math.max(1, n);
            float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) { x[i] += shift; min = Math.min(min, x[i]); max = Math.max(max, x[i]); }
            // Se passar das bordas: encolhe (camada larga demais) ou desliza para dentro de [-0.95, 0.95].
            float width = max - min;
            float mid = (min + max) / 2;
            float scale = width > 1.9f ? 1.9f / width : 1f;
            float slide = 0;
            if (scale == 1f) {
                if (max > 0.95f) slide = 0.95f - max;
                if (min < -0.95f) slide = -0.95f - min;
            }
            for (int i = 0; i < n; i++) {
                PerkDef p = layer.get(i);
                p.x = scale < 1f ? (x[i] - mid) * scale : x[i] + slide;
                p.y = maxDepth == 0 ? 0 : 0.92f - 1.84f * d / maxDepth;
            }
        }
    }

    private static int depthOf(PerkDef p, Map<String, PerkDef> local, Map<PerkDef, Integer> memo) {
        Integer cached = memo.get(p);
        if (cached != null) return cached;
        int d = 0;
        for (String r : p.requires) d = Math.max(d, depthOf(local.get(r), local, memo) + 1);
        memo.put(p, d);
        return d;
    }

    private static float parentX(PerkDef p, Map<String, PerkDef> local) {
        if (p.requires.isEmpty()) return 0;
        float sum = 0;
        for (String r : p.requires) sum += local.get(r).x;
        return sum / p.requires.size() + p.index * 0.001f;
    }

    // ---------- Árvores ----------

    static {
        tree(Skill.ONE_HANDED)
                .perk("golpe_firme", lv(0, 20, 40), ROOT, st(SWORD_DAMAGE, 0.10, 0.20, 0.30))
                .perk("lamina_afiada", lv(20), req("golpe_firme"), st(SWORD_BLEED, 0.15))
                .perk("corte_profundo", lv(40), req("lamina_afiada"), st(SWORD_BLEED_STRONG, 1))
                .perk("danca_laminas", lv(60), req("corte_profundo"), st(SWORD_COMBO, 0.05))
                .perk("olho_duelista", lv(20), req("golpe_firme"), st(SWORD_CRIT, 0.08))
                .perk("golpe_preciso", lv(40), req("olho_duelista"), st(SWORD_CRIT_MULT, 1.75))
                .perk("executor", lv(70), req("golpe_preciso"), st(SWORD_EXECUTE, 0.40))
                .perk("passo_leve", lv(25), req("golpe_firme"), st(SWORD_STAMINA, 0.30))
                .perk("investida", lv(45), req("passo_leve"), st(SWORD_CHARGE, 0.30))
                .perk("contra_ataque", lv(55), req("passo_leve"), st(COUNTER, 0.50))
                .perk("furia", lv(50), req("golpe_firme"), st(SWORD_RAGE, 0.20))
                .perk("lamina_envenenada", lv(65), req("lamina_afiada"), st(SWORD_POISON, 0.10))
                .perk("desarmar", lv(75), req("executor"), st(SWORD_DISARM, 0.05))
                .perk("sede_sangue", lv(85), req("danca_laminas"), st(SWORD_LIFESTEAL, 2))
                .perk("mestre_laminas", lv(100), req("danca_laminas", "executor", "investida"),
                        st(SWORD_ATTACK_SPEED, 0.10), st(SWORD_DOUBLE, 0.10))
                .done();

        tree(Skill.TWO_HANDED)
                .perk("barbaro", lv(0, 20, 40), ROOT, st(HEAVY_DAMAGE, 0.10, 0.20, 0.30))
                .perk("rachador", lv(20), req("barbaro"), st(HEAVY_ARMOR_PEN, 0.25))
                .perk("esmagador", lv(45), req("rachador"), st(HEAVY_ARMOR_PEN, 0.50))
                .perk("golpe_pesado", lv(25), req("barbaro"), st(HEAVY_KNOCKBACK, 1.0))
                .perk("terremoto", lv(60), req("golpe_pesado"), st(HEAVY_CLEAVE, 0.50))
                .perk("talho_selvagem", lv(20), req("barbaro"), st(HEAVY_BLEED, 0.15))
                .perk("hemorragia", lv(50), req("talho_selvagem"), st(HEAVY_BLEED_STRONG, 1))
                .perk("ceifador", lv(70), req("hemorragia"), st(HEAVY_KILL_STRENGTH, 1))
                .perk("folego_guerra", lv(30), req("barbaro"), st(HEAVY_STAMINA, 0.30))
                .perk("implacavel", lv(55), req("folego_guerra"), st(HEAVY_HIT_STAMINA, 3))
                .perk("critico_brutal", lv(45), req("barbaro"), st(HEAVY_CRIT, 0.08))
                .perk("golpe_devastador", lv(75), req("critico_brutal"), st(HEAVY_CRIT_MULT, 2.0))
                .perk("pele_ferro", lv(65), req("golpe_pesado"), st(HEAVY_HOLD_ARMOR, 2))
                .perk("lanceiro", lv(40), req("barbaro"), st(TRIDENT_THROWN, 0.25))
                .perk("senhor_guerra", lv(100), req("esmagador", "terremoto", "golpe_devastador"),
                        st(HEAVY_DAMAGE, 0.15), st(HEAVY_KB_RES, 0.5))
                .done();

        tree(Skill.ARCHERY)
                .perk("olho_aguia", lv(0, 20, 40), ROOT, st(RANGED_DAMAGE, 0.10, 0.20, 0.30))
                .perk("mao_rapida", lv(25), req("olho_aguia"), st(DRAW_SPEED, 0.20))
                .perk("disparo_duplo", lv(65), req("mao_rapida"), st(DOUBLE_SHOT, 0.15))
                .perk("chuva_flechas", lv(85), req("disparo_duplo"), st(ARROW_FAN, 1))
                .perk("recuperacao", lv(30), req("olho_aguia"), st(ARROW_RECOVER, 0.33))
                .perk("aljava_generosa", lv(60), req("recuperacao"), st(ARROW_RECOVER, 0.50))
                .perk("cacador", lv(20), req("olho_aguia"), st(RANGED_VS_BEASTS, 0.25))
                .perk("tiro_cabeca", lv(45), req("olho_aguia"), st(HEADSHOT, 0.50))
                .perk("flecha_paralisante", lv(55), req("tiro_cabeca"), st(ARROW_SLOW, 0.10))
                .perk("perfurante", lv(70), req("tiro_cabeca"), st(RANGED_ARMOR_PEN, 0.50))
                .perk("besteiro", lv(20), req("olho_aguia"), st(CROSSBOW_DAMAGE, 0.20))
                .perk("vento_favor", lv(40), req("olho_aguia"), st(ARROW_VELOCITY, 0.25))
                .perk("tiro_furtivo", lv(50), req("olho_aguia"), st(SNEAK_RANGED_MULT, 2.0))
                .perk("olho_falcao", lv(75), req("vento_favor"), st(LONG_SHOT, 0.20))
                .perk("mestre_arqueiro", lv(100), req("chuva_flechas", "aljava_generosa", "olho_falcao"),
                        st(RANGED_DAMAGE, 0.15), st(RANGED_CRIT, 0.20))
                .done();

        tree(Skill.BLOCK)
                .perk("guardiao", lv(0, 30), ROOT, st(BLOCK_STAMINA, 3, 6))
                .perk("retaliacao", lv(25), req("guardiao"), st(BLOCK_REFLECT, 0.25))
                .perk("espinhos_aco", lv(60), req("retaliacao"), st(BLOCK_REFLECT, 0.50))
                .perk("empurrao_escudo", lv(25), req("guardiao"), st(BLOCK_KNOCKBACK, 1))
                .perk("golpe_escudo", lv(50), req("empurrao_escudo"), st(BLOCK_STUN, 1))
                .perk("defletor", lv(45), req("guardiao"), st(BLOCK_DEFLECT, 1))
                .perk("guarda_elemental", lv(55), req("defletor"), st(SHIELD_ELEMENTAL, 0.30))
                .perk("contra_golpe", lv(35), req("guardiao"), st(COUNTER, 0.30))
                .perk("recuperacao_rapida", lv(30), req("guardiao"), st(SHIELD_RECOVERY, 0.50))
                .perk("muralha_inabalavel", lv(40), req("recuperacao_rapida"), st(BLOCK_KB_IMMUNE, 1))
                .perk("escudo_reforcado", lv(45), req("guardiao"), st(SHIELD_DURABILITY, 0.50))
                .perk("bastiao", lv(65), req("escudo_reforcado"), st(SHIELD_HOLD_ARMOR, 2))
                .perk("disciplina", lv(50), req("guardiao"), st(BLOCK_RESISTANCE, 1))
                .perk("ultimo_recurso", lv(80), req("disciplina"), st(BLOCK_LAST_STAND, 1))
                .perk("fortaleza_imortal", lv(100), req("espinhos_aco", "golpe_escudo", "ultimo_recurso"),
                        st(BLOCK_PASSIVE, 0.15))
                .done();

        tree(Skill.HEAVY_ARMOR)
                .perk("juggernaut", lv(0, 20, 40), ROOT, st(HEAVY_ARMOR_BONUS, 0.10, 0.20, 0.30))
                .perk("conjunto_pesado", lv(30), req("juggernaut"), st(HEAVY_SET_ARMOR, 0.10))
                .perk("tanque", lv(50), req("conjunto_pesado"), st(HEAVY_SET_KB, 0.30))
                .perk("pele_rocha", lv(70), req("tanque"), st(HEAVY_SET_DR, 0.10))
                .perk("couraca", lv(20), req("juggernaut"), st(HEAVY_PROJ_DR, 0.15))
                .perk("muralha_ferro", lv(35), req("couraca"), st(HEAVY_EXPLOSION_DR, 0.20))
                .perk("inquebravel", lv(30), req("juggernaut"), st(HEAVY_DURABILITY, 0.30))
                .perk("forja_viva", lv(65), req("inquebravel"), st(HEAVY_REPAIR, 1))
                .perk("resiliencia", lv(40), req("juggernaut"), st(HEAVY_DEBUFF_RES, 0.30))
                .perk("contra_impacto", lv(45), req("juggernaut"), st(FALL_DR, 0.25))
                .perk("refletir", lv(60), req("pele_rocha"), st(HEAVY_REFLECT, 0.10))
                .perk("peito_aco", lv(55), req("conjunto_pesado"), st(HEAVY_MELEE_DR, 0.10))
                .perk("imparavel", lv(75), req("tanque"), st(HEAVY_EXPLOSION_KB, 1))
                .perk("rocha_viva", lv(85), req("refletir"), st(HEAVY_LOW_HP_ARMOR, 0.20))
                .perk("colosso", lv(100), req("pele_rocha", "forja_viva", "rocha_viva"), st(HEAVY_COLOSSUS, 1))
                .done();

        tree(Skill.LIGHT_ARMOR)
                .perk("agilidade", lv(0, 20, 40), ROOT, st(LIGHT_ARMOR_BONUS, 0.10, 0.20, 0.30))
                .perk("esquiva", lv(25), req("agilidade"), st(DODGE, 0.05))
                .perk("reflexos", lv(50), req("esquiva"), st(DODGE, 0.05))
                .perk("sombra_veloz", lv(75), req("reflexos"), st(DODGE, 0.05))
                .perk("passos_leves", lv(20), req("agilidade"), st(LIGHT_SPEED, 0.05))
                .perk("vento", lv(45), req("passos_leves"), st(LIGHT_SPEED, 0.05))
                .perk("pes_ageis", lv(30), req("agilidade"), st(FALL_DR, 0.30))
                .perk("conjunto_leve", lv(35), req("agilidade"), st(LIGHT_SET_ARMOR, 0.15))
                .perk("folego_leve", lv(40), req("agilidade"), st(LIGHT_STAMINA_REGEN, 0.25))
                .perk("mergulhador", lv(35), req("agilidade"), st(OXYGEN, 1))
                .perk("rolamento", lv(55), req("pes_ageis"), st(SNEAK_FALL_DR, 0.50))
                .perk("esquiva_projeteis", lv(60), req("esquiva"), st(DODGE_PROJECTILE, 0.15))
                .perk("leve_pena", lv(65), req("vento"), st(LIGHT_JUMP, 1))
                .perk("vento_cortante", lv(80), req("sombra_veloz"), st(DODGE_COUNTER, 0.50))
                .perk("mestre_esquiva", lv(100), req("sombra_veloz", "leve_pena", "vento_cortante"),
                        st(DODGE, 0.10), st(DODGE_STAMINA, 10))
                .done();

        tree(Skill.SNEAK)
                .perk("furtivo", lv(0, 20, 40), ROOT, st(SNEAK_VISIBILITY, 0.15, 0.25, 0.35))
                .perk("ataque_furtivo", lv(20), req("furtivo"), st(SNEAK_MELEE_MULT, 2.5))
                .perk("assassino", lv(50), req("ataque_furtivo"), st(SNEAK_SWORD_MULT, 3.5))
                .perk("golpe_mortal", lv(80), req("assassino"), st(SNEAK_SWORD_MULT, 6.0))
                .perk("arqueiro_oculto", lv(30), req("furtivo"), st(SNEAK_RANGED_MULT, 2.0))
                .perk("passos_silenciosos", lv(25), req("furtivo"), st(SILENT, 1))
                .perk("sombra", lv(40), req("passos_silenciosos"), st(SNEAK_SPEED, 0.30))
                .perk("desaparecer", lv(60), req("sombra"), st(VANISH, 1))
                .perk("ladrao", lv(35), req("furtivo"), st(SNEAK_LOOT, 0.30))
                .perk("evasao", lv(45), req("furtivo"), st(EVASION, 1))
                .perk("sem_rastro", lv(55), req("passos_silenciosos"), st(NO_ENDERMAN, 1))
                .perk("veneno_furtivo", lv(65), req("ataque_furtivo"), st(SNEAK_POISON, 1))
                .perk("fuga", lv(50), req("evasao"), st(SNEAK_PROJ_DR, 0.20))
                .perk("mestre_sombras", lv(85), req("desaparecer"), st(SHADOW_INVIS, 1))
                .perk("fantasma", lv(100), req("golpe_mortal", "sem_rastro", "mestre_sombras"), st(PHANTOM, 1))
                .done();

        tree(Skill.ATHLETICS)
                .perk("folego", lv(0, 20, 40), ROOT, st(STAMINA_MAX, 10, 20, 30))
                .perk("corredor", lv(20), req("folego"), st(SPRINT_COST, 0.20))
                .perk("maratonista", lv(50), req("corredor"), st(SPRINT_COST, 0.20))
                .perk("velocista", lv(35), req("folego"), st(SPRINT_SPEED, 0.05))
                .perk("relampago", lv(70), req("velocista"), st(SPRINT_SPEED, 0.05))
                .perk("saltador", lv(25), req("folego"), st(JUMP_COST, 0.50))
                .perk("pernas_mola", lv(45), req("saltador"), st(JUMP_BOOST, 0.10))
                .perk("queda_suave", lv(30), req("folego"), st(FALL_DR, 0.25))
                .perk("gato", lv(60), req("queda_suave"), st(FALL_DR, 0.25), st(SAFE_FALL, 3))
                .perk("nadador", lv(20), req("folego"), st(SWIM_SPEED, 0.20))
                .perk("peixe", lv(50), req("nadador"), st(OXYGEN, 1), st(SWIM_FREE, 1))
                .perk("escalador", lv(40), req("folego"), st(CLIMB_SPEED, 0.30))
                .perk("recuperacao_atl", lv(55), req("folego"), st(STAMINA_REGEN, 0.25))
                .perk("segundo_folego", lv(75), req("recuperacao_atl"), st(SECOND_WIND, 1))
                .perk("incansavel", lv(100), req("maratonista", "relampago", "segundo_folego"), st(TIRELESS, 1))
                .done();

        tree(Skill.MINING)
                .perk("braco_forte", lv(0, 20, 40), ROOT, st(MINING_SPEED, 0.15, 0.30, 0.45))
                .perk("prospector", lv(20), req("braco_forte"), st(ORE_DOUBLE, 0.05))
                .perk("veio_rico", lv(50), req("prospector"), st(ORE_DOUBLE, 0.07))
                .perk("coracao_montanha", lv(80), req("veio_rico"), st(ORE_DOUBLE, 0.08))
                .perk("faro_minerio", lv(30), req("braco_forte"), st(ORE_SENSE, 4))
                .perk("visao_profunda", lv(60), req("faro_minerio"), st(ORE_SENSE, 8))
                .perk("escavador", lv(25), req("braco_forte"), st(STONE_SPEED, 0.25))
                .perk("mineracao_veio", lv(55), req("escavador"), st(VEIN_MINE, 12))
                .perk("resistente", lv(35), req("braco_forte"), st(PICK_DURABILITY, 0.25))
                .perk("fundidor", lv(45), req("braco_forte"), st(AUTO_SMELT, 0.20))
                .perk("geologo", lv(40), req("braco_forte"), st(ORE_XP, 0.50))
                .perk("pedras_preciosas", lv(65), req("veio_rico"), st(GEM_FIND, 0.01))
                .perk("morador_cavernas", lv(30), req("braco_forte"), st(CAVE_VISION, 1))
                .perk("incansavel_min", lv(50), req("resistente"), st(MINING_NO_HUNGER, 1))
                .perk("mestre_minerador", lv(100), req("coracao_montanha", "visao_profunda", "mineracao_veio"),
                        st(MINING_SPEED, 0.30), st(ORE_DOUBLE, 0.10))
                .done();

        tree(Skill.WOODCUTTING)
                .perk("machado_afiado", lv(0, 20, 40), ROOT, st(WOOD_SPEED, 0.20, 0.40, 0.60))
                .perk("colheita_farta", lv(25), req("machado_afiado"), st(LOG_EXTRA, 0.15))
                .perk("floresta_generosa", lv(60), req("colheita_farta"), st(LOG_EXTRA, 0.15))
                .perk("derrubada", lv(40), req("machado_afiado"), st(TREE_FELL, 1))
                .perk("silvicultor", lv(55), req("derrubada"), st(AUTO_REPLANT, 1))
                .perk("mudas", lv(20), req("machado_afiado"), st(LEAF_DROPS, 0.50))
                .perk("pomar_dourado", lv(50), req("mudas"), st(GOLDEN_APPLE, 0.005))
                .perk("durabilidade_machado", lv(30), req("machado_afiado"), st(AXE_DURABILITY, 0.25))
                .perk("carpinteiro", lv(35), req("machado_afiado"), st(PLANKS_BONUS, 1))
                .perk("golpe_lenhador", lv(45), req("machado_afiado"), st(AXE_COMBAT, 0.10))
                .perk("ritmo", lv(40), req("machado_afiado"), st(CHOP_HASTE, 1))
                .perk("coletor", lv(50), req("colheita_farta"), st(MUSHROOM_LOGS, 1))
                .perk("seiva", lv(65), req("mudas"), st(APPLE_HEAL, 4))
                .perk("espirito_floresta", lv(80), req("silvicultor"), st(FELL_XP, 1))
                .perk("guardiao_floresta", lv(100), req("floresta_generosa", "silvicultor", "espirito_floresta"),
                        st(FELL_FREE, 1))
                .done();

        tree(Skill.SMITHING)
                .perk("aprendiz", lv(0, 20, 40), ROOT, st(SMITH_DURABILITY, 0.10, 0.20, 0.30))
                .perk("ferreiro_ferro", lv(20), req("aprendiz"), st(QUALITY_IRON, 1))
                .perk("ferreiro_diamante", lv(50), req("ferreiro_ferro"), st(QUALITY_DIAMOND, 1))
                .perk("ferreiro_netherite", lv(80), req("ferreiro_diamante"), st(QUALITY_NETHERITE, 1))
                .perk("forja_arcana", lv(30), req("aprendiz"), st(SMITH_ENCHANT, 0.10), st(SMITH_ENCHANT_LEVEL, 10))
                .perk("encantador", lv(55), req("forja_arcana"), st(SMITH_ENCHANT, 0.20), st(SMITH_ENCHANT_LEVEL, 20))
                .perk("forja_lendaria", lv(85), req("encantador"), st(SMITH_ENCHANT, 0.30), st(SMITH_ENCHANT_LEVEL, 30))
                .perk("reparador", lv(25), req("aprendiz"), st(ANVIL_DISCOUNT, 0.50))
                .perk("bigorna_resistente", lv(60), req("reparador"), st(ANVIL_SAFE, 1))
                .perk("economia", lv(35), req("aprendiz"), st(CRAFT_REFUND, 0.15))
                .perk("afiacao", lv(40), req("ferreiro_ferro"), st(SMITH_WEAPON_DAMAGE, 1))
                .perk("tempera", lv(45), req("ferreiro_ferro"), st(SMITH_ARMOR, 1))
                .perk("reciclagem", lv(50), req("economia"), st(RECYCLE, 2))
                .perk("assinatura", lv(65), req("afiacao", "tempera"), st(MIN_FINE, 1))
                .perk("obra_prima", lv(100), req("ferreiro_netherite", "forja_lendaria", "assinatura"),
                        st(MASTERWORK, 0.10))
                .done();

        tree(Skill.FARMING)
                .perk("mao_verde", lv(0, 20, 40), ROOT, st(HARVEST_EXTRA, 0.10, 0.20, 0.30))
                .perk("fertilizante", lv(20), req("mao_verde"), st(BONEMEAL_SAVE, 0.50))
                .perk("crescimento", lv(50), req("fertilizante"), st(CROP_AURA, 4), st(CROP_AURA_SPEED, 0.20))
                .perk("mestre_campo", lv(85), req("crescimento"), st(CROP_AURA, 8), st(CROP_AURA_SPEED, 1.0))
                .perk("replantio", lv(25), req("mao_verde"), st(RIGHT_CLICK_HARVEST, 1))
                .perk("colheita_area", lv(55), req("replantio"), st(AREA_HARVEST, 1))
                .perk("criador", lv(25), req("mao_verde"), st(TWINS, 0.25))
                .perk("criador_experiente", lv(50), req("criador"), st(BABY_GROWTH, 0.50))
                .perk("acougueiro", lv(40), req("criador"), st(MEAT_EXTRA, 1))
                .perk("pastor", lv(30), req("criador"), st(WOOL_EXTRA, 1))
                .perk("cozinheiro", lv(30), req("mao_verde"), st(FOOD_BONUS, 0.20))
                .perk("banquete", lv(60), req("cozinheiro"), st(FOOD_REGEN, 1))
                .perk("pescador", lv(35), req("mao_verde"), st(FISHING_EXTRA, 0.20))
                .perk("safra_dourada", lv(75), req("mestre_campo", "colheita_area"), st(GOLDEN_HARVEST, 0.02))
                .perk("senhor_terra", lv(100), req("mestre_campo", "criador_experiente", "safra_dourada"),
                        st(FARM_MASTER, 1))
                .done();
    }

    /** Todos os perks, em ordem estável (usada na sincronização). */
    public static List<PerkDef> all() {
        List<PerkDef> out = new ArrayList<>();
        for (Skill s : Skill.values()) out.addAll(of(s));
        return out;
    }

    static {
        // Sanidade: cada requisito precisa existir na mesma árvore.
        for (Skill s : Skill.values()) {
            for (PerkDef p : of(s)) {
                for (String r : p.requires) {
                    if (byLocal(s, r) == null) throw new IllegalStateException("Perk " + p.id + " requer inexistente: " + r);
                }
                if (p.levels.length == 0) throw new IllegalStateException("Perk sem níveis: " + p.id + Arrays.toString(p.levels));
            }
        }
    }

    private PerkTrees() {}
}
