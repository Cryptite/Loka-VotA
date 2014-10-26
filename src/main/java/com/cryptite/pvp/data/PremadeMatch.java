package com.cryptite.pvp.data;

import java.util.List;

public class PremadeMatch {
    public final String type;
    public String id;
    public List<String> blue;
    public List<String> red;
    public List<String> spectators;
    public Boolean active;

    public PremadeMatch(String type) {
        this.type = type;
    }
}
