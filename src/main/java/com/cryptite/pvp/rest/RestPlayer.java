package com.cryptite.pvp.rest;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;

@SuppressWarnings("FieldCanBeLocal")
public class RestPlayer {

    private final String name;
    private final String title;
    private int valleyKills = 0;
    private int valleyDeaths = 0;
    private int valleyCaps = 0;
    private int valleyWins = 0;
    private int valleyLosses = 0;
    private int valleyScore = 0;
    private int arrowShots = 0;
    private int arrowHits = 0;


    public RestPlayer(LokaVotA plugin, PvPPlayer p) {
        this.valleyKills = p.valleyKills;
        this.valleyDeaths = p.valleyDeaths;
        this.valleyCaps = p.valleyCaps;
        this.valleyWins = p.valleyWins;
        this.valleyLosses = p.valleyLosses;
        this.valleyScore = p.valleyScore;
        this.arrowShots = p.arrowsFired;
        this.arrowHits = p.arrowHits;
        this.name = p.name;
        this.title = p.title;
    }
}
