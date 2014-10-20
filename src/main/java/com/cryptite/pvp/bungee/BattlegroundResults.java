package com.cryptite.pvp.bungee;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BattlegroundResults {
    private String name;
    private Map<String, Integer> playerScores = new HashMap<>();
    public Set<String> winners;
    public Set<String> losers;

    public BattlegroundResults(String name, Map<String, Integer> scores) {
        this.name = name;
        this.playerScores = scores;
    }
}
