package com.cryptite.pvp.rest;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;

@SuppressWarnings("FieldCanBeLocal")
public class RestPlayer {

    private final String name;
    private final String title;
    private int arenarating = 1500;
    private int arenarating2v2 = 1500;
    private int highestrating = 1500;
    private int highestrating2v2 = 1500;
    private int arenawins = 0;
    private int arenawins2v2 = 0;
    private int arenalosses = 0;
    private int arenalosses2v2 = 0;
    private int streak = 0;
    private int streak2v2 = 0;
    private int valleyKills = 0;
    private int valleyDeaths = 0;
    private int valleyCaps = 0;
    private int valleyWins = 0;
    private int valleyLosses = 0;
    private int valleyScore = 0;
    private int overloadKills = 0;
    private int overloadDeaths = 0;
    private int overloadOverloads = 0;
    private int overloadWins = 0;
    private int overloadLosses = 0;
    private int overloadScore = 0;
    private int arrowShots = 0;
    private int arrowHits = 0;


    public RestPlayer(LokaVotA plugin, PvPPlayer p) {
        this.arenarating = p.arenarating;
        this.arenarating2v2 = p.arenarating2v2;
        this.highestrating = p.highestrating;
        this.highestrating2v2 = p.highestrating2v2;
        this.arenawins = p.arenawins;
        this.arenawins2v2 = p.arenawins2v2;
        this.arenalosses = p.arenalosses;
        this.arenalosses2v2 = p.arenalosses2v2;
        this.streak = p.streak;
        this.streak2v2 = p.streak2v2;
        this.valleyKills = p.valleyKills;
        this.valleyDeaths = p.valleyDeaths;
        this.valleyCaps = p.valleyCaps;
        this.valleyWins = p.valleyWins;
        this.valleyLosses = p.valleyLosses;
        this.valleyScore = p.valleyScore;
        this.overloadKills = p.overloadKills;
        this.overloadDeaths = p.overloadDeaths;
        this.overloadOverloads = p.overloadOverloads;
        this.overloadWins = p.overloadWins;
        this.overloadLosses = p.overloadLosses;
        this.overloadScore = p.overloadScore;
        this.arrowShots = p.arrowsFired;
        this.arrowHits = p.arrowHits;
        this.name = p.name;
        this.title = p.title;
    }
}
