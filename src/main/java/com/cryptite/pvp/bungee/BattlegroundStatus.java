package com.cryptite.pvp.bungee;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"FieldCanBeLocal", "CanBeFinal"})
public class BattlegroundStatus {
    private final String game;
    public int redSize = 0;
    public int blueSize = 0;
    public Map<String, Set<String>> players = new HashMap<>();
    public Boolean active = false;

    public BattlegroundStatus(String game) {
        this.game = game;
    }
}
