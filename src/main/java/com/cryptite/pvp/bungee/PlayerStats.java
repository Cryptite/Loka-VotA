package com.cryptite.pvp.bungee;

import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.talents.Talent;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class PlayerStats {

    public final String name;
    boolean talentsSaved = true;
    public String rank;
    public String town;
    public String alliance;
    public List<Talent> talents = new ArrayList<>();
    private int prowess = 0;
    private int valleyScore = 0;
    private int valleyWins = 0;
    private int valleyLosses = 0;
    private int valleyKills = 0;
    private int valleyDeaths = 0;
    private int valleyCaps = 0;
    private int bgRating = 1500;
    private int arrowShots = 0;
    private int arrowHits = 0;

    public PlayerStats(PvPPlayer p) {
        this.prowess = p.prowess;
        this.valleyScore = p.valleyScore;
        this.valleyWins = p.valleyWins;
        this.valleyLosses = p.valleyLosses;
        this.valleyKills = p.valleyKills;
        this.valleyDeaths = p.valleyDeaths;
        this.valleyCaps = p.valleyCaps;
        this.bgRating = p.bgRating;
        this.arrowShots = p.arrowsFired;
        this.arrowHits = p.arrowHits;
        this.name = p.name;
        this.talents = p.talents;
        this.talentsSaved = p.talentsSaved;
    }
}
