package com.cryptite.pvp.json;

import java.util.List;

public class Match {
    public final String type;
    public String p1, p2, p3, p4;
    public String id;
    public List<String> bgplayers;
    public List<String> spectators;
    public Boolean active;

    public Match(String type) {
        this.type = type;
    }
}
