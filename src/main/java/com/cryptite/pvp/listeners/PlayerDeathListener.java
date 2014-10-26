package com.cryptite.pvp.listeners;

import com.cryptite.pvp.Battleground;
import com.cryptite.pvp.LokaVotA;
import com.cryptite.pvp.PvPPlayer;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.vota.VotA;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class PlayerDeathListener implements Listener {
    private final Logger log = Logger.getLogger("LokaVotA-PlayerQuitListener");
    private final LokaVotA plugin;

    public PlayerDeathListener(LokaVotA plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        //Remove arena player from pool so they can be properly recreated next time they join.
        Player player = e.getEntity();
        PvPPlayer p = plugin.getAccount(e.getEntity().getName());

        if (p.bg != null) {

            ChatColor playerColor;
            ChatColor killerColor;

            String deathMessage = e.getDeathMessage();
            e.setDeathMessage("");

            if (plugin.lastDamageCause.containsKey(p.name)) {
                deathMessage = parseDeath(p.name);
            }

            Battleground bg = plugin.getBG(p.bg);

            if (player.getKiller() != null) {
                PvPPlayer killer = plugin.getAccount(player.getKiller().getName());
                if (bg != null) {

                    if (bg.redTeam.containsKey(p.name) && bg.blueTeam.containsKey(killer.name)) {
                        playerColor = ChatColor.RED;
                        killerColor = ChatColor.BLUE;
                        deathMessage = deathMessage.replace(p.name, playerColor + p.name + ChatColor.WHITE);
                        deathMessage = deathMessage.replace(killer.name, killerColor + killer.name + ChatColor.WHITE);

                        bg.messageAllPlayers(deathMessage, null);

                        //Only kills in VotA contribute to team score
                        if (bg instanceof VotA) {
                            bg.blueScore++;
                            ((VotA) bg).updateScore();
                            ((VotA) bg).checkScoreResult();
                        }
                    } else if (bg.blueTeam.containsKey(p.name) && bg.redTeam.containsKey(killer.name)) {
                        playerColor = ChatColor.BLUE;
                        killerColor = ChatColor.RED;
                        deathMessage = deathMessage.replace(p.name, playerColor + p.name + ChatColor.WHITE);
                        deathMessage = deathMessage.replace(killer.name, killerColor + killer.name + ChatColor.WHITE);
                        bg.messageAllPlayers(deathMessage, null);

                        //Only kills in VotA contribute to team score
                        if (bg instanceof VotA) {
                            bg.redScore++;
                            ((VotA) bg).updateScore();
                            ((VotA) bg).checkScoreResult();
                        }
                    }
                    //Increment player's kill count
                    bg.updatePlayerScore(killer.name, 1);

                    if (bg instanceof VotA) {
                        p.valleyDeaths++;
                        p.increment("valleyDeaths");
                        killer.increment("valleyKills");
                    }
                }

                checkVictimAchievements(p, killer);
                checkKillerActiveTalent(killer);

                //If they have the sword damage talent, activate the speed buff
                if (killer.hasTalent(Talent.SWORD_DAMAGE)) {
                    killer.activateTalent(Talent.SWORD_DAMAGE);
                }
            } else {
                if (bg != null) {
                    if (bg.blueTeam.containsKey(p.name)) {
                        deathMessage = deathMessage.replace(p.name, ChatColor.BLUE + p.name + ChatColor.WHITE);
                        bg.messageAllPlayers(deathMessage, null);
                    } else if (bg.redTeam.containsKey(p.name)) {
                        deathMessage = deathMessage.replace(p.name, ChatColor.RED + p.name + ChatColor.WHITE);
                        bg.messageAllPlayers(deathMessage, null);
                    }
                }
            }

            stripDeathDrops(e);
        }

        p.grooveStacks = 0;
    }

    private String parseDeath(String victim) {
        String message = victim;
        switch (plugin.lastDamageCause.get(victim)) {
            case SILENCE:
                message += " stepped on " + plugin.lastDamager.get(victim) + "'s Ender Charge";
                break;
            case EXPLOSIVE_ARROW:
                message += " was destroyed by " + plugin.lastDamager.get(victim) + "'s Explosive Arrow";
                break;
            case QUAKE:
                message += " was fell to " + plugin.lastDamager.get(victim) + "'s Quake";
                break;
            case LUNGE:
                message += " was cut down by " + plugin.lastDamager.get(victim);
                break;
            case GROOVE:
                message += " was sniped by " + plugin.lastDamager.get(victim);
                break;
            case SWORD_DAMAGE:
                message += " was slain by " + plugin.lastDamager.get(victim);
                break;
            case HOT_BOW:
                message += " was shot by " + plugin.lastDamager.get(victim);
                break;
            case POTION_SLINGER:
                message += " was killed by " + plugin.lastDamager.get(victim) + " using magic";
                break;
        }

        //Since they died and we're announcing it, can remove them from these maps.
        plugin.clearDamageCause(victim);

        return message;
    }

    void checkVictimAchievements(PvPPlayer victim, PvPPlayer killer) {
        if (victim.hasTalentActive(Talent.EXPLOSIVE_ARROW)) {
            plugin.bungee.sendMessage("loka", killer.name + ".pvp.killexplosivearrowplayer", "Achievement");
        } else if (victim.hasTalentActive(Talent.RALLYING_CRY)) {
            plugin.bungee.sendMessage("loka", killer.name + ".pvp.killvanishedplayer", "Achievement");
        }

        if (victim.getPlayer().hasPotionEffect(PotionEffectType.POISON)) {
            plugin.bungee.sendMessage("loka", killer.name + ".pvp.killtrappedplayer", "Achievement");
        }

//        if (victim.talentAbilities.get(Talent.SILENCE).enderCharge != null) {
//            victim.talentAbilities.get(Talent.SILENCE).armEnderCharge();
//        }
    }

    void checkKillerActiveTalent(PvPPlayer killer) {
        if (killer.hasTalentActive(Talent.EXPLOSIVE_ARROW)) {
            plugin.bungee.sendMessage("loka", killer.name + ".pvp.killplayerwithexplosivearrow", "Achievement");
        } else if (killer.hasTalentActive(Talent.LUNGE)) {
            plugin.bungee.sendMessage("loka", killer.name + ".pvp.killwithlunge", "Achievement");
        }
    }

    void stripDeathDrops(PlayerDeathEvent e) {
        //Only drop arrows
        List<ItemStack> drops = new ArrayList<>();
        for (ItemStack item : e.getDrops()) {
            if (item == null || item.getType().equals(Material.AIR)) continue;

            if (item.getType().equals(Material.ARROW)) {
                item.setAmount((int) (item.getAmount() * .2));
                drops.add(item);
            }
        }

        e.getDrops().clear();

        for (ItemStack item : drops) {
            if (item.getType().equals(Material.AIR)) continue; //Why the hell I have to do this....

            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), item);
        }
    }
}
