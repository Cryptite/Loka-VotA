package com.cryptite.pvp.utils;

public class Rating {
    public static Double getRatingChange(int winnerRating, int loserRating) {
        return (36 * (1 - (1 / (1 + (Math.pow(10, (((double) loserRating - (double) winnerRating) / 400)))))));
    }

}
