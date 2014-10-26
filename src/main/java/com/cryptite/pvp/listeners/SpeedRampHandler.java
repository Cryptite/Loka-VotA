package com.cryptite.pvp.listeners;

import com.cryptite.pvp.CustomEffect;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cryptite.pvp.utils.LocationUtils.*;
import static java.util.Arrays.asList;

public class SpeedRampHandler implements Runnable {
    private final LokaVotA plugin;
    private Combat combat;

    private final Map<String, Location> ramps = new HashMap<>();
    private Map<String, List<Block>> rampLocations = new HashMap<>();
    private Map<String, List<String>> usedRamps = new HashMap<>();

    public SpeedRampHandler(LokaVotA plugin) {
        this.plugin = plugin;
        combat = new Combat(plugin);
    }

    @Override
    public void run() {
        for (String owner : ramps.keySet()) {
            Location chargeLocation = ramps.get(owner);
            for (Player friendly : chargeLocation.getWorld().getPlayers()) {
                //Enemies can't use this ramp
                if (!combat.sameTeam(owner, friendly.getName()) || usedRamp(owner, friendly.getName())) continue;

                double distance = friendly.getLocation().distance(chargeLocation);
                if (distance <= 3) {
                    plugin.effect(chargeLocation, CustomEffect.LAVA);

                    //Tell player via sound his trap went off
                    playCustomSound(friendly, "Resolve");

                    //Apply speed buff
                    if (plugin.getAccount(friendly.getName()).hasTalent(Talent.FLEET_FOOTED)) {
                        //If they have Fleet Footed, they already have Speed 1 permanently up, so up to 2.
                        combat.addPotionEffect(friendly, PotionEffectType.SPEED, 2, 5);

                        //After ability is over, we need to give them fleet footed back
                        plugin.scheduler.runTaskLater(plugin,
                                () -> {
                                    if (!friendly.isDead()) {
                                        combat.addPotionEffect(friendly, PotionEffectType.SPEED, 0, 50000);
                                    }
                                }, 20 * 5 + 2
                        );
                    } else {
                        combat.addPotionEffect(friendly, PotionEffectType.SPEED, 2, 5);
                    }
                }
            }
        }
    }

    public void createRamp(Player creator, String team) {
        playWorldCustomSound(creator.getLocation(), "SpeedZone", 15);

        ramps.put(creator.getName(), creator.getLocation());

        int woolID = Material.WOOL.getId();
        int woolColorID = 8;
        if (team != null) {
            if (team.equals("red")) {
                woolColorID = 14;
            } else {
                woolColorID = 3;
            }
        }

        //Get a 3x3 area of blocks
        rampLocations.put(creator.getName(),
                getBlocksFromRegion(creator.getLocation().add(1, 0, 1),
                        creator.getLocation().subtract(1, 0, 1)));

        //Play trap FX for owner
        final int finalWoolColorID = woolColorID;
        int rampId = plugin.scheduler.scheduleSyncRepeatingTask(plugin,
                () -> {
                    for (Block b : rampLocations.get(creator.getName())) {
                        plugin.blockEffect(b.getLocation().add(.5f, .5f, .5f), CustomEffect.BLOCKDUST, woolID, finalWoolColorID, 3, .1f, 2f);
                    }
                }, 5, 5);

        plugin.scheduler.runTaskLater(plugin, () -> plugin.scheduler.cancelTask(rampId), 20 * 6);

        //Cancel the ramp
        plugin.scheduler.runTaskLater(plugin, () -> {
            usedRamps.remove(creator.getName());
            ramps.remove(creator.getName());
            rampLocations.remove(creator.getName());
        }, 20 * 7);

        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, 5);
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
