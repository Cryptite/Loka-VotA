package com.cryptite.pvp.talents;

import java.util.HashMap;
import java.util.Map;

public enum Talent {
    HOT_BOW(0),
    ARCHER(9),
    GROOVE(18),
    FREEZING_TRAP(27),
    EXPLOSIVE_ARROW(36),
    FLAMING_SWORD(1),
    SLOW(10),
    LIFE_STEAL(19),
    SWORD_DAMAGE(28),
    LUNGE(37),
    BLAST_PROTECTION(3),
    FRESH_START(12),
    THORNS(21),
    ENDER_HOOK(30),
    QUAKE(39),
    REFLEXES(5),
    FIRE_PROTECTION(14),
    IRON_FORM(23),
    HARDENED(32),
    LAST_STAND(41),
    FLEET_FOOTED(7),
    BOUNCY(16),
    POTION_SLINGER(25),
    RALLYING_CRY(34),
    SILENCE(43),
    BANDAGE(8),
    POTENCY(17),
    HOLY_GRENADE(26),
    LIFE_SHIELD(35),
    HEAL_ARROW(44),
    SPAWN(100);

    private final int talentSlot;
    private static final Map<Integer, Talent> map = new HashMap<>();

    static {
        for (Talent talentEnum : Talent.values()) {
            map.put(talentEnum.talentSlot, talentEnum);
        }
    }

    private Talent(final int talentSlot) {
        this.talentSlot = talentSlot;
    }

    public static Talent valueOf(int slot) {
        return map.get(slot);
    }

    public static int toInt(Talent t) {
        for (Map.Entry<Integer, Talent> entry : map.entrySet()) {
            if (entry.getValue().equals(t)) return entry.getKey();
        }
        return 0;
    }
}