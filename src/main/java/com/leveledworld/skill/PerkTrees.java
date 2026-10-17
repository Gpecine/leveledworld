package com.leveledworld.skill;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.leveledworld.skill.Stat.*;

/**
 * Todas as árvores de perks (docs/plano-skills.md). Comum a cliente e servidor.
 * As estrelas de cada árvore desenham a silhueta da skill (espada, machadão, arco, escudo...): as posições vêm de
 * {@code at(x, y)} e as linhas são os requisitos, então cada requisito também é um traço do desenho.
 */
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

        /** Posição da estrela do último perk, em raios da árvore (x em [-1.2, 1.2], y em [-0.9, 0.9], y para baixo). */
        Builder at(float x, float y) {
            PerkDef p = perks.get(perks.size() - 1);
            p.x = x;
            p.y = y;
            return this;
        }

        void done() {
            TREES.put(skill, Collections.unmodifiableList(perks));
            for (PerkDef p : perks) BY_ID.put(p.id, p);
        }
    }

    private static Builder tree(Skill skill) { return new Builder(skill); }

    // ---------- Árvores ----------

    static {
        tree(Skill.ONE_HANDED)
                .perk("golpe_firme", lv(0, 20, 40), ROOT, st(SWORD_DAMAGE, 0.10, 0.20, 0.30)).at(0, 0.86f)
                .perk("lamina_afiada", lv(25), req("olho_duelista"), st(SWORD_BLEED, 0.15)).at(-0.42f, 0.38f)
                .perk("corte_profundo", lv(40), req("lamina_afiada"), st(SWORD_BLEED_STRONG, 1)).at(-0.22f, 0.1f)
                .perk("danca_laminas", lv(60), req("corte_profundo"), st(SWORD_COMBO, 0.05)).at(-0.22f, -0.2f)
                .perk("olho_duelista", lv(20), req("passo_leve"), st(SWORD_CRIT, 0.08)).at(0, 0.38f)
                .perk("golpe_preciso", lv(40), req("investida"), st(SWORD_CRIT_MULT, 1.75)).at(0.22f, 0.1f)
                .perk("executor", lv(70), req("golpe_preciso"), st(SWORD_EXECUTE, 0.40)).at(0.22f, -0.2f)
                .perk("passo_leve", lv(20), req("golpe_firme"), st(SWORD_STAMINA, 0.30)).at(0, 0.62f)
                .perk("investida", lv(30), req("olho_duelista"), st(SWORD_CHARGE, 0.30)).at(0.42f, 0.38f)
                .perk("contra_ataque", lv(55), req("investida"), st(COUNTER, 0.50)).at(0.8f, 0.28f)
                .perk("furia", lv(50), req("olho_duelista"), st(SWORD_RAGE, 0.20)).at(0, -0.32f)
                .perk("lamina_envenenada", lv(65), req("lamina_afiada"), st(SWORD_POISON, 0.10)).at(-0.8f, 0.28f)
                .perk("desarmar", lv(75), req("executor"), st(SWORD_DISARM, 0.05)).at(0.22f, -0.5f)
                .perk("sede_sangue", lv(85), req("danca_laminas"), st(SWORD_LIFESTEAL, 2)).at(-0.22f, -0.5f)
                .perk("mestre_laminas", lv(100), req("sede_sangue", "desarmar", "furia"),
                        st(SWORD_ATTACK_SPEED, 0.10), st(SWORD_DOUBLE, 0.10)).at(0, -0.86f)
                .done();

        tree(Skill.TWO_HANDED)
                .perk("barbaro", lv(0, 20, 40), ROOT, st(HEAVY_DAMAGE, 0.10, 0.20, 0.30)).at(0, 0.86f)
                .perk("rachador", lv(40), req("golpe_pesado"), st(HEAVY_ARMOR_PEN, 0.25)).at(-0.26f, -0.44f)
                .perk("esmagador", lv(55), req("rachador"), st(HEAVY_ARMOR_PEN, 0.50)).at(-0.74f, -0.86f)
                .perk("golpe_pesado", lv(30), req("folego_guerra"), st(HEAVY_KNOCKBACK, 1.0)).at(0, -0.3f)
                .perk("terremoto", lv(65), req("esmagador"), st(HEAVY_CLEAVE, 0.50)).at(-0.96f, -0.3f)
                .perk("talho_selvagem", lv(40), req("golpe_pesado"), st(HEAVY_BLEED, 0.15)).at(0.26f, -0.44f)
                .perk("hemorragia", lv(55), req("talho_selvagem"), st(HEAVY_BLEED_STRONG, 1)).at(0.74f, -0.86f)
                .perk("ceifador", lv(70), req("hemorragia"), st(HEAVY_KILL_STRENGTH, 1)).at(0.96f, -0.3f)
                .perk("folego_guerra", lv(25), req("lanceiro"), st(HEAVY_STAMINA, 0.30)).at(0, 0.18f)
                .perk("implacavel", lv(45), req("golpe_pesado"), st(HEAVY_HIT_STAMINA, 3)).at(-0.26f, -0.14f)
                .perk("critico_brutal", lv(45), req("golpe_pesado"), st(HEAVY_CRIT, 0.08)).at(0.26f, -0.14f)
                .perk("golpe_devastador", lv(75), req("ceifador", "critico_brutal"), st(HEAVY_CRIT_MULT, 2.0)).at(0.74f, 0.26f)
                .perk("pele_ferro", lv(70), req("terremoto", "implacavel"), st(HEAVY_HOLD_ARMOR, 2)).at(-0.74f, 0.26f)
                .perk("lanceiro", lv(20), req("barbaro"), st(TRIDENT_THROWN, 0.25)).at(0, 0.52f)
                .perk("senhor_guerra", lv(100), req("golpe_pesado"),
                        st(HEAVY_DAMAGE, 0.15), st(HEAVY_KB_RES, 0.5)).at(0, -0.84f)
                .done();

        tree(Skill.ARCHERY)
                .perk("olho_aguia", lv(0, 20, 40), ROOT, st(RANGED_DAMAGE, 0.10, 0.20, 0.30)).at(-0.58f, 0)
                .perk("mao_rapida", lv(25), req("olho_aguia"), st(DRAW_SPEED, 0.20)).at(-0.5f, 0.4f)
                .perk("disparo_duplo", lv(65), req("flecha_paralisante", "vento_favor", "aljava_generosa"), st(DOUBLE_SHOT, 0.15)).at(0.56f, 0)
                .perk("chuva_flechas", lv(85), req("disparo_duplo"), st(ARROW_FAN, 1)).at(0.86f, 0.18f)
                .perk("recuperacao", lv(30), req("mao_rapida"), st(ARROW_RECOVER, 0.33)).at(-0.26f, 0.72f)
                .perk("aljava_generosa", lv(60), req("recuperacao"), st(ARROW_RECOVER, 0.50)).at(0.12f, 0.86f)
                .perk("cacador", lv(20), req("olho_aguia"), st(RANGED_VS_BEASTS, 0.25)).at(-0.5f, -0.4f)
                .perk("tiro_cabeca", lv(45), req("olho_aguia"), st(HEADSHOT, 0.50)).at(-0.92f, -0.18f)
                .perk("flecha_paralisante", lv(55), req("tiro_furtivo"), st(ARROW_SLOW, 0.10)).at(0.18f, 0)
                .perk("perfurante", lv(70), req("olho_aguia"), st(RANGED_ARMOR_PEN, 0.50)).at(-0.92f, 0.18f)
                .perk("besteiro", lv(30), req("cacador"), st(CROSSBOW_DAMAGE, 0.20)).at(-0.26f, -0.72f)
                .perk("vento_favor", lv(40), req("besteiro"), st(ARROW_VELOCITY, 0.25)).at(0.12f, -0.86f)
                .perk("tiro_furtivo", lv(35), req("olho_aguia"), st(SNEAK_RANGED_MULT, 2.0)).at(-0.2f, 0)
                .perk("olho_falcao", lv(75), req("disparo_duplo"), st(LONG_SHOT, 0.20)).at(0.86f, -0.18f)
                .perk("mestre_arqueiro", lv(100), req("tiro_cabeca", "perfurante"),
                        st(RANGED_DAMAGE, 0.15), st(RANGED_CRIT, 0.20)).at(-1.18f, 0)
                .done();

        tree(Skill.BLOCK)
                .perk("guardiao", lv(0, 30), ROOT, st(BLOCK_STAMINA, 3, 6)).at(0, -0.12f)
                .perk("retaliacao", lv(25), req("guardiao"), st(BLOCK_REFLECT, 0.25)).at(-0.42f, -0.12f)
                .perk("espinhos_aco", lv(60), req("escudo_reforcado"), st(BLOCK_REFLECT, 0.50)).at(-0.75f, -0.8f)
                .perk("empurrao_escudo", lv(25), req("guardiao"), st(BLOCK_KNOCKBACK, 1)).at(0.42f, -0.12f)
                .perk("golpe_escudo", lv(50), req("empurrao_escudo"), st(BLOCK_STUN, 1)).at(0.82f, -0.3f)
                .perk("defletor", lv(35), req("guardiao"), st(BLOCK_DEFLECT, 1)).at(0, 0.38f)
                .perk("guarda_elemental", lv(55), req("disciplina"), st(SHIELD_ELEMENTAL, 0.30)).at(-0.36f, 0.62f)
                .perk("contra_golpe", lv(55), req("disciplina"), st(COUNTER, 0.30)).at(0.36f, 0.62f)
                .perk("recuperacao_rapida", lv(30), req("guardiao"), st(SHIELD_RECOVERY, 0.50)).at(0, -0.5f)
                .perk("muralha_inabalavel", lv(55), req("contra_golpe", "golpe_escudo"), st(BLOCK_KB_IMMUNE, 1)).at(0.68f, 0.22f)
                .perk("escudo_reforcado", lv(45), req("retaliacao"), st(SHIELD_DURABILITY, 0.50)).at(-0.82f, -0.3f)
                .perk("bastiao", lv(65), req("golpe_escudo"), st(SHIELD_HOLD_ARMOR, 2)).at(0.75f, -0.8f)
                .perk("disciplina", lv(50), req("defletor"), st(BLOCK_RESISTANCE, 1)).at(0, 0.88f)
                .perk("ultimo_recurso", lv(80), req("guarda_elemental", "escudo_reforcado"), st(BLOCK_LAST_STAND, 1)).at(-0.68f, 0.22f)
                .perk("fortaleza_imortal", lv(100), req("espinhos_aco", "bastiao", "recuperacao_rapida"),
                        st(BLOCK_PASSIVE, 0.15)).at(0, -0.86f)
                .done();

        tree(Skill.HEAVY_ARMOR)
                .perk("juggernaut", lv(0, 20, 40), ROOT, st(HEAVY_ARMOR_BONUS, 0.10, 0.20, 0.30)).at(0, 0.86f)
                .perk("conjunto_pesado", lv(30), req("juggernaut"), st(HEAVY_SET_ARMOR, 0.10)).at(0, 0.12f)
                .perk("tanque", lv(50), req("contra_impacto"), st(HEAVY_SET_KB, 0.30)).at(-0.8f, -0.76f)
                .perk("pele_rocha", lv(70), req("tanque"), st(HEAVY_SET_DR, 0.10)).at(-0.3f, -0.84f)
                .perk("couraca", lv(20), req("juggernaut"), st(HEAVY_PROJ_DR, 0.15)).at(-0.55f, 0.84f)
                .perk("muralha_ferro", lv(35), req("couraca"), st(HEAVY_EXPLOSION_DR, 0.20)).at(-0.6f, -0.32f)
                .perk("inquebravel", lv(30), req("juggernaut"), st(HEAVY_DURABILITY, 0.30)).at(0.55f, 0.84f)
                .perk("forja_viva", lv(55), req("peito_aco"), st(HEAVY_REPAIR, 1)).at(0.92f, -0.1f)
                .perk("resiliencia", lv(40), req("muralha_ferro"), st(HEAVY_DEBUFF_RES, 0.30)).at(-0.92f, -0.1f)
                .perk("contra_impacto", lv(45), req("resiliencia"), st(FALL_DR, 0.25)).at(-1.12f, -0.4f)
                .perk("refletir", lv(60), req("forja_viva"), st(HEAVY_REFLECT, 0.10)).at(1.12f, -0.4f)
                .perk("peito_aco", lv(45), req("inquebravel"), st(HEAVY_MELEE_DR, 0.10)).at(0.6f, -0.32f)
                .perk("imparavel", lv(75), req("refletir"), st(HEAVY_EXPLOSION_KB, 1)).at(0.8f, -0.76f)
                .perk("rocha_viva", lv(85), req("imparavel"), st(HEAVY_LOW_HP_ARMOR, 0.20)).at(0.3f, -0.84f)
                .perk("colosso", lv(100), req("pele_rocha", "rocha_viva", "conjunto_pesado"), st(HEAVY_COLOSSUS, 1)).at(0, -0.58f)
                .done();

        tree(Skill.LIGHT_ARMOR)
                .perk("agilidade", lv(0, 20, 40), ROOT, st(LIGHT_ARMOR_BONUS, 0.10, 0.20, 0.30)).at(-0.75f, 0.86f)
                .perk("esquiva", lv(25), req("agilidade"), st(DODGE, 0.05)).at(-0.63f, 0.39f)
                .perk("reflexos", lv(50), req("esquiva_projeteis", "vento"), st(DODGE, 0.05)).at(-0.2f, -0.41f)
                .perk("sombra_veloz", lv(75), req("reflexos", "leve_pena"), st(DODGE, 0.05)).at(0.11f, -0.67f)
                .perk("passos_leves", lv(20), req("agilidade"), st(LIGHT_SPEED, 0.05)).at(-0.4f, 0.43f)
                .perk("vento", lv(45), req("passos_leves"), st(LIGHT_SPEED, 0.05)).at(-0.05f, 0)
                .perk("pes_ageis", lv(30), req("agilidade"), st(FALL_DR, 0.30)).at(-0.31f, 0.64f)
                .perk("conjunto_leve", lv(35), req("mergulhador", "vento"), st(LIGHT_SET_ARMOR, 0.15)).at(0.38f, 0.07f)
                .perk("folego_leve", lv(40), req("conjunto_leve", "leve_pena"), st(LIGHT_STAMINA_REGEN, 0.25)).at(0.57f, -0.29f)
                .perk("mergulhador", lv(35), req("pes_ageis", "passos_leves"), st(OXYGEN, 1)).at(0.07f, 0.39f)
                .perk("rolamento", lv(55), req("folego_leve"), st(SNEAK_FALL_DR, 0.50)).at(0.66f, -0.62f)
                .perk("esquiva_projeteis", lv(60), req("esquiva", "passos_leves"), st(DODGE_PROJECTILE, 0.15)).at(-0.45f, -0.04f)
                .perk("leve_pena", lv(65), req("vento"), st(LIGHT_JUMP, 1)).at(0.3f, -0.43f)
                .perk("vento_cortante", lv(80), req("sombra_veloz"), st(DODGE_COUNTER, 0.50)).at(0.41f, -0.82f)
                .perk("mestre_esquiva", lv(100), req("leve_pena", "vento_cortante", "rolamento"),
                        st(DODGE, 0.10), st(DODGE_STAMINA, 10)).at(0.65f, -0.86f)
                .done();

        tree(Skill.SNEAK)
                .perk("furtivo", lv(0, 20, 40), ROOT, st(SNEAK_VISIBILITY, 0.15, 0.25, 0.35)).at(-1.2f, 0)
                .perk("ataque_furtivo", lv(20), req("furtivo"), st(SNEAK_MELEE_MULT, 2.5)).at(-0.7f, -0.42f)
                .perk("assassino", lv(50), req("ataque_furtivo"), st(SNEAK_SWORD_MULT, 3.5)).at(0, -0.6f)
                .perk("golpe_mortal", lv(80), req("assassino"), st(SNEAK_SWORD_MULT, 6.0)).at(0.7f, -0.42f)
                .perk("arqueiro_oculto", lv(30), req("furtivo"), st(SNEAK_RANGED_MULT, 2.0)).at(-0.32f, 0)
                .perk("passos_silenciosos", lv(25), req("furtivo"), st(SILENT, 1)).at(-0.7f, 0.42f)
                .perk("sombra", lv(40), req("passos_silenciosos"), st(SNEAK_SPEED, 0.30)).at(0, 0.6f)
                .perk("desaparecer", lv(60), req("sombra"), st(VANISH, 1)).at(0.7f, 0.42f)
                .perk("ladrao", lv(35), req("arqueiro_oculto"), st(SNEAK_LOOT, 0.30)).at(-0.16f, -0.28f)
                .perk("evasao", lv(45), req("arqueiro_oculto"), st(EVASION, 1)).at(-0.16f, 0.28f)
                .perk("sem_rastro", lv(55), req("fuga", "veneno_furtivo"), st(NO_ENDERMAN, 1)).at(0.32f, 0)
                .perk("veneno_furtivo", lv(65), req("ladrao"), st(SNEAK_POISON, 1)).at(0.16f, -0.28f)
                .perk("fuga", lv(50), req("evasao"), st(SNEAK_PROJ_DR, 0.20)).at(0.16f, 0.28f)
                .perk("mestre_sombras", lv(85), req("golpe_mortal", "desaparecer"), st(SHADOW_INVIS, 1)).at(1.2f, 0)
                .perk("fantasma", lv(100), req("arqueiro_oculto", "sem_rastro"), st(PHANTOM, 1)).at(0, 0)
                .done();

        tree(Skill.ATHLETICS)
                .perk("folego", lv(0, 20, 40), ROOT, st(STAMINA_MAX, 10, 20, 30)).at(0, -0.08f)
                .perk("corredor", lv(20), req("folego"), st(SPRINT_COST, 0.20)).at(-0.08f, 0.18f)
                .perk("maratonista", lv(50), req("velocista"), st(SPRINT_COST, 0.20)).at(0.28f, 0.8f)
                .perk("velocista", lv(35), req("corredor"), st(SPRINT_SPEED, 0.05)).at(0.36f, 0.36f)
                .perk("relampago", lv(70), req("maratonista"), st(SPRINT_SPEED, 0.05)).at(0.58f, 0.84f)
                .perk("saltador", lv(25), req("corredor"), st(JUMP_COST, 0.50)).at(-0.32f, 0.52f)
                .perk("pernas_mola", lv(45), req("saltador"), st(JUMP_BOOST, 0.10)).at(-0.76f, 0.6f)
                .perk("queda_suave", lv(45), req("escalador"), st(FALL_DR, 0.25)).at(-0.18f, -0.42f)
                .perk("gato", lv(60), req("pernas_mola"), st(FALL_DR, 0.25), st(SAFE_FALL, 3)).at(-0.9f, 0.32f)
                .perk("nadador", lv(20), req("folego"), st(SWIM_SPEED, 0.20)).at(0.1f, -0.34f)
                .perk("peixe", lv(50), req("escalador"), st(OXYGEN, 1), st(SWIM_FREE, 1)).at(0.55f, -0.42f)
                .perk("escalador", lv(40), req("nadador"), st(CLIMB_SPEED, 0.30)).at(0.2f, -0.6f)
                .perk("recuperacao_atl", lv(55), req("peixe"), st(STAMINA_REGEN, 0.25)).at(0.84f, -0.64f)
                .perk("segundo_folego", lv(75), req("queda_suave"), st(SECOND_WIND, 1)).at(-0.46f, -0.16f)
                .perk("incansavel", lv(100), req("escalador"), st(TIRELESS, 1)).at(0.32f, -0.86f)
                .done();

        tree(Skill.MINING)
                .perk("braco_forte", lv(0, 20, 40), ROOT, st(MINING_SPEED, 0.15, 0.30, 0.45)).at(0, -0.5f)
                .perk("prospector", lv(20), req("braco_forte"), st(ORE_DOUBLE, 0.05)).at(-0.5f, -0.44f)
                .perk("veio_rico", lv(50), req("prospector"), st(ORE_DOUBLE, 0.07)).at(-1.15f, -0.12f)
                .perk("coracao_montanha", lv(80), req("pedras_preciosas"), st(ORE_DOUBLE, 0.08)).at(-0.42f, -0.8f)
                .perk("faro_minerio", lv(30), req("braco_forte"), st(ORE_SENSE, 4)).at(0.5f, -0.44f)
                .perk("visao_profunda", lv(60), req("fundidor"), st(ORE_SENSE, 8)).at(0.42f, -0.8f)
                .perk("escavador", lv(25), req("braco_forte"), st(STONE_SPEED, 0.25)).at(0, -0.22f)
                .perk("mineracao_veio", lv(55), req("incansavel_min"), st(VEIN_MINE, 12)).at(0, 0.86f)
                .perk("resistente", lv(35), req("escavador"), st(PICK_DURABILITY, 0.25)).at(0, 0.06f)
                .perk("fundidor", lv(45), req("geologo"), st(AUTO_SMELT, 0.20)).at(0.82f, -0.6f)
                .perk("geologo", lv(40), req("faro_minerio"), st(ORE_XP, 0.50)).at(1.15f, -0.12f)
                .perk("pedras_preciosas", lv(65), req("veio_rico"), st(GEM_FIND, 0.01)).at(-0.82f, -0.6f)
                .perk("morador_cavernas", lv(40), req("resistente"), st(CAVE_VISION, 1)).at(0, 0.34f)
                .perk("incansavel_min", lv(50), req("morador_cavernas"), st(MINING_NO_HUNGER, 1)).at(0, 0.6f)
                .perk("mestre_minerador", lv(100), req("coracao_montanha", "visao_profunda"),
                        st(MINING_SPEED, 0.30), st(ORE_DOUBLE, 0.10)).at(0, -0.88f)
                .done();

        tree(Skill.WOODCUTTING)
                .perk("machado_afiado", lv(0, 20, 40), ROOT, st(WOOD_SPEED, 0.20, 0.40, 0.60)).at(0, 0.86f)
                .perk("colheita_farta", lv(35), req("carpinteiro"), st(LOG_EXTRA, 0.15)).at(-0.44f, 0.22f)
                .perk("floresta_generosa", lv(60), req("colheita_farta"), st(LOG_EXTRA, 0.15)).at(-0.9f, 0.44f)
                .perk("derrubada", lv(40), req("ritmo"), st(TREE_FELL, 1)).at(-0.3f, -0.26f)
                .perk("silvicultor", lv(55), req("derrubada"), st(AUTO_REPLANT, 1)).at(-0.62f, 0.02f)
                .perk("mudas", lv(35), req("carpinteiro"), st(LEAF_DROPS, 0.50)).at(0.44f, 0.22f)
                .perk("pomar_dourado", lv(50), req("mudas"), st(GOLDEN_APPLE, 0.005)).at(0.9f, 0.44f)
                .perk("durabilidade_machado", lv(20), req("machado_afiado"), st(AXE_DURABILITY, 0.25)).at(0, 0.42f)
                .perk("carpinteiro", lv(30), req("durabilidade_machado"), st(PLANKS_BONUS, 1)).at(0, -0.06f)
                .perk("golpe_lenhador", lv(45), req("ritmo"), st(AXE_COMBAT, 0.10)).at(0.36f, -0.52f)
                .perk("ritmo", lv(35), req("carpinteiro"), st(CHOP_HASTE, 1)).at(0, -0.5f)
                .perk("coletor", lv(50), req("ritmo"), st(MUSHROOM_LOGS, 1)).at(0.3f, -0.26f)
                .perk("seiva", lv(65), req("coletor"), st(APPLE_HEAL, 4)).at(0.62f, 0.02f)
                .perk("espirito_floresta", lv(80), req("ritmo"), st(FELL_XP, 1)).at(-0.36f, -0.52f)
                .perk("guardiao_floresta", lv(100), req("espirito_floresta", "golpe_lenhador", "ritmo"),
                        st(FELL_FREE, 1)).at(0, -0.88f)
                .done();

        tree(Skill.SMITHING)
                .perk("aprendiz", lv(0, 20, 40), ROOT, st(SMITH_DURABILITY, 0.10, 0.20, 0.30)).at(0.2f, -0.55f)
                .perk("ferreiro_ferro", lv(20), req("aprendiz"), st(QUALITY_IRON, 1)).at(-0.45f, -0.55f)
                .perk("ferreiro_diamante", lv(50), req("ferreiro_ferro"), st(QUALITY_DIAMOND, 1)).at(-1.2f, -0.42f)
                .perk("ferreiro_netherite", lv(80), req("ferreiro_diamante"), st(QUALITY_NETHERITE, 1)).at(-0.66f, -0.2f)
                .perk("forja_arcana", lv(40), req("economia"), st(SMITH_ENCHANT, 0.10), st(SMITH_ENCHANT_LEVEL, 10)).at(0.38f, -0.1f)
                .perk("encantador", lv(55), req("reciclagem"), st(SMITH_ENCHANT, 0.20), st(SMITH_ENCHANT_LEVEL, 20)).at(0.68f, 0.5f)
                .perk("forja_lendaria", lv(85), req("encantador"), st(SMITH_ENCHANT, 0.30), st(SMITH_ENCHANT_LEVEL, 30)).at(0.8f, 0.84f)
                .perk("reparador", lv(25), req("aprendiz"), st(ANVIL_DISCOUNT, 0.50)).at(0.85f, -0.55f)
                .perk("bigorna_resistente", lv(70), req("assinatura"), st(ANVIL_SAFE, 1)).at(-0.8f, 0.84f)
                .perk("economia", lv(35), req("reparador"), st(CRAFT_REFUND, 0.15)).at(0.85f, -0.22f)
                .perk("afiacao", lv(40), req("ferreiro_ferro", "ferreiro_netherite"), st(SMITH_WEAPON_DAMAGE, 1)).at(-0.38f, -0.1f)
                .perk("tempera", lv(45), req("afiacao"), st(SMITH_ARMOR, 1)).at(-0.38f, 0.36f)
                .perk("reciclagem", lv(50), req("forja_arcana"), st(RECYCLE, 2)).at(0.38f, 0.36f)
                .perk("assinatura", lv(65), req("tempera"), st(MIN_FINE, 1)).at(-0.68f, 0.5f)
                .perk("obra_prima", lv(100), req("bigorna_resistente", "forja_lendaria"),
                        st(MASTERWORK, 0.10)).at(0, 0.84f)
                .done();

        tree(Skill.FARMING)
                .perk("mao_verde", lv(0, 20, 40), ROOT, st(HARVEST_EXTRA, 0.10, 0.20, 0.30)).at(0, 0.86f)
                .perk("fertilizante", lv(20), req("mao_verde"), st(BONEMEAL_SAVE, 0.50)).at(0, 0.48f)
                .perk("crescimento", lv(50), req("cozinheiro"), st(CROP_AURA, 4), st(CROP_AURA_SPEED, 0.20)).at(0, -0.18f)
                .perk("mestre_campo", lv(70), req("crescimento"), st(CROP_AURA, 8), st(CROP_AURA_SPEED, 1.0)).at(0, -0.5f)
                .perk("replantio", lv(25), req("fertilizante"), st(RIGHT_CLICK_HARVEST, 1)).at(-0.34f, 0.22f)
                .perk("colheita_area", lv(55), req("crescimento"), st(AREA_HARVEST, 1)).at(-0.34f, -0.44f)
                .perk("criador", lv(25), req("fertilizante"), st(TWINS, 0.25)).at(0.34f, 0.22f)
                .perk("criador_experiente", lv(50), req("crescimento"), st(BABY_GROWTH, 0.50)).at(0.34f, -0.44f)
                .perk("acougueiro", lv(40), req("cozinheiro"), st(MEAT_EXTRA, 1)).at(0.34f, -0.12f)
                .perk("pastor", lv(30), req("cozinheiro"), st(WOOL_EXTRA, 1)).at(-0.34f, -0.12f)
                .perk("cozinheiro", lv(30), req("fertilizante"), st(FOOD_BONUS, 0.20)).at(0, 0.14f)
                .perk("banquete", lv(75), req("mestre_campo"), st(FOOD_REGEN, 1)).at(0.3f, -0.76f)
                .perk("pescador", lv(35), req("mao_verde"), st(FISHING_EXTRA, 0.20)).at(0.55f, 0.62f)
                .perk("safra_dourada", lv(80), req("mestre_campo"), st(GOLDEN_HARVEST, 0.02)).at(-0.3f, -0.76f)
                .perk("senhor_terra", lv(100), req("mestre_campo", "safra_dourada", "banquete"),
                        st(FARM_MASTER, 1)).at(0, -0.88f)
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
                if (Float.isNaN(p.x) || Float.isNaN(p.y)) throw new IllegalStateException("Perk sem posição: " + p.id);
                if (p.levels.length == 0) throw new IllegalStateException("Perk sem níveis: " + p.id + Arrays.toString(p.levels));
            }
        }
    }

    private PerkTrees() {}
}
