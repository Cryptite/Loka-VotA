package com.cryptite.pvp.talents;

import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.utils.Combat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Aura implements Runnable {
    private PvPPlayer p;
    private Combat combat;
    private int taskID;

    public Aura(PvPPlayer p) {
        this.p = p;

        LokaVotA plugin = (LokaVotA) Bukkit.getPluginManager().getPlugin("LokaVotA");
        combat = new Combat(plugin);

        taskID = plugin.scheduler.scheduleSyncRepeatingTask(plugin, this, 60, 20 * 45);
    }

    @Override
    public void run() {
        if (p.getPlayer() == null) return;

        for (Player friendly : p.getPlayer().getWorld().getPlayers()) {
            if (friendly == null
                    || combat.spectator(friendly.getName())
                    || !combat.sameTeam(friendly.getName(), p.name)
                    || friendly.getLocation().distance(p.getLocation()) > 15) continue;

            System.out.println(p.name + " restored " + friendly.getName());
            combat.addHarmingPotion(friendly, Talent.SILENCE);
            combat.addHealingPotion(friendly);
        }
    }

    public void cancel() {
        System.out.println("Stopping " + p.name + "'s aura");
        Bukkit.getScheduler().cancelTask(taskID);
    }
}
