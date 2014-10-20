package com.cryptite.pvp.listeners;

import com.cryptite.pvp.CustomEffect;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class SpeedRampHandler implements Runnable {
    private final LokaVotA plugin;
    private Combat combat;

    public final Map<String, Location> ramps = new HashMap<>();
    public final Map<String, Integer> rampTasks = new HashMap<>();
    private Map<String, List<String>> usedRamps = new HashMap<>();

    public SpeedRampHandler(LokaVotA plugin) {
        this.plugin = plugin;
        combat = new Combat(plugin);
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 5, 5);
    }

    @Override
    public void run() {
        for (String owner : ramps.keySet()) {
            Location chargeLocation = ramps.get(owner);
            for (Player friendly : chargeLocation.getWorld().getPlayers()) {
                //Enemies can't use this ramp
                if (!combat.sameTeam(owner, friendly.getName()) && !usedRamp(owner, friendly.getName())) continue;

                double distance = friendly.getLocation().distance(chargeLocation);
                if (distance <= 3) {
                    plugin.effect(chargeLocation, CustomEffect.LAVA);

                    //Tell player via sound his trap went off
                    friendly.playSound(friendly.getLocation(), "Resolve", 2, 1);
                    friendly.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 5, 2));
                }
            }
        }
    }

    private Boolean usedRamp(String rampOwner, String rampUser) {
        if (usedRamps.containsKey(rampOwner)) {
            if (usedRamps.get(rampOwner).contains(rampUser)) return true;
            else {
                List<String> usedList = usedRamps.get(rampOwner);
                usedList.add(rampUser);
                return false;
            }
        } else {
            List<String> newUsed = new ArrayList<>(asList(rampUser));
            usedRamps.put(rampOwner, newUsed);
            return false;
        }
    }
}
