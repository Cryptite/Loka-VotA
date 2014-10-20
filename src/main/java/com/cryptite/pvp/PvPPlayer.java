package com.cryptite.pvp;

import com.cryptite.pvp.bungee.PlayerStats;
import com.cryptite.pvp.rest.Rest;
import com.cryptite.pvp.rest.RestPlayer;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.talents.TalentAbility;
import com.cryptite.pvp.utils.Rope;
import org.bukkit.*;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.bukkit.ChatColor.stripColor;
import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class PvPPlayer {
    private final Logger log = Logger.getLogger("Artifact-PvPPlayer");

    private final LokaVotA plugin;
    public Location location;
    public World world;
    public String town;
    public String alliance;
    public String townRank;
    public String bg;
    public double damage = 0;
    public final String name;
    public String title;
    public String rank;
    public Boolean townOwner = false;
    public Boolean ready = false;
    public Boolean spectator = false;
    public Color playerColor;
    public final ConfigFile config;
    public List<Talent> talents = new ArrayList<>();
    public boolean talentsSaved = false;
    public final int talentRespecs = 0;

    //Stats
    public int arrowHits = 0;
    public int arrowsFired = 0;
    public int valleyKills = 0;
    public int valleyDeaths = 0;
    public int valleyCaps = 0;
    public int valleyWins = 0;
    public int valleyLosses = 0;
    public int valleyScore = 0;
    public int bgRating = 1500;
    private long deserterTime = 0;
    public int prowess = 0;

    //Healthbar
    public Objective healthBar;

    //Talents
    public final Map<Talent, TalentAbility> talentAbilities = new HashMap<>();
    public final Map<Material, Talent> talentMaterials = new HashMap<>();
    public final Map<ItemStack, Integer> inventoryOrder = new HashMap<>();
    public Rope hook;

    //Talent-Groove
    public int grooveStacks = 0;
    private int maxHarmingPotions = 2;
    public boolean offense;
    public String team;

    //Achievement trackers
    public final List<String> lastStandAttackers = new ArrayList<>();
    public Boolean provingGrounds = false;

    public PvPPlayer(LokaVotA plugin, String name) {
        this.name = name;
        this.config = new ConfigFile(plugin, "players/" + name + ".yml");
        this.plugin = plugin;
    }

    public void save() {
        config.set("name", name);
        config.set("rank", rank);
        config.set("townowner", townOwner);
        config.set("arrowhits", arrowHits);
        config.set("arrowsfired", arrowsFired);
        config.set("valleyKills", valleyKills);
        config.set("valleyDeaths", valleyDeaths);
        config.set("valleyCaps", valleyCaps);
        config.set("valleyWins", valleyWins);
        config.set("valleyLosses", valleyLosses);
        config.set("bgRating", bgRating);
        config.set("deserterTime", deserterTime);
        config.save();

        //Send their updated stats along with the player
        PlayerStats stats = new PlayerStats(this);
        plugin.bungee.sendMessage(new net.minecraft.util.com.google.gson.Gson().toJson(stats), "PlayerUpdate");
        //Saving also updates Loka with all the information.
    }

    public void saveInventoryOrder() {
        inventoryOrder.clear();
        log.info("Saving inventory order for " + name);
        for (int i = 0; i <= getPlayer().getInventory().getSize(); i++) {
            ItemStack item = getPlayer().getInventory().getItem(i);
            if (item != null && !item.getType().equals(Material.COAL)) {
                inventoryOrder.put(item, i);
//                config.set("inventoryorder", null);
            }
        }

        for (ItemStack item : inventoryOrder.keySet()) {
            if (item.getType().equals(Material.POTION)) {
                if (item.getDurability() == 16389) {
                    config.set("inventoryorder.POTION-HEAL", inventoryOrder.get(item));
                } else {
                    config.set("inventoryorder.POTION-HARM", inventoryOrder.get(item));
                }
            } else {
                config.set("inventoryorder." + item.getType().toString(), inventoryOrder.get(item));
            }
        }
        config.save();
    }

    public void postPlayer() {
        final PvPPlayer p = this;
        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin,
                () -> {
                    //Loka Website stuff
                    Gson gson = new Gson();
                    if (Rest.put("player", name, gson.toJson(new RestPlayer(plugin, p)))) {
                        log.info("[REST-Player] " + name + " successfully put");
                    } else {
                        log.info("[REST-Player] " + name + " failed to put");
                        log.info("Json: " + gson.toJson(new RestPlayer(plugin, p)));
                    }
                }, 20
        );
    }

    public void load() {
        rank = config.get("rank", rank);
        townOwner = Boolean.parseBoolean(config.get("townowner", townOwner));
        arrowHits = Integer.parseInt(config.get("arrowhits", arrowHits));
        arrowsFired = Integer.parseInt(config.get("arrowsfired", arrowsFired));
        valleyKills = Integer.parseInt(config.get("valleyKills", valleyKills));
        valleyDeaths = Integer.parseInt(config.get("valleyDeaths", valleyDeaths));
        valleyCaps = Integer.parseInt(config.get("valleyCaps", valleyCaps));
        valleyWins = Integer.parseInt(config.get("valleyWins", valleyWins));
        valleyLosses = Integer.parseInt(config.get("valleyLosses", valleyLosses));
        bgRating = Integer.parseInt(config.get("bgRating", bgRating));
        deserterTime = Long.parseLong(config.get("deserterTime", deserterTime));
        valleyScore = getVotaScore();
        loadInventoryOrder();

//        loadFromDB();
    }

    private void loadFromDB() {
        ResultSet result = plugin.db.getPlayer(this);
        if (result == null) {
            System.out.println("Nothing gotten from db");
            return;
        }

        try {
            town = result.getString("town");
            System.out.println("Got town = " + town + " from db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void loadInventoryOrder() {
        inventoryOrder.clear();
        if (config.configFile.exists() && config.getAll("inventoryorder", null) != null) {
            for (String item : config.getAll("inventoryorder", null)) {
                int slot = Integer.parseInt(config.get("inventoryorder." + item, null));
                ItemStack i;
                if (item.contains("-HEAL")) {
                    i = new ItemStack(Material.POTION);
                    i.setDurability((short) 16389);
                } else if (item.contains("-HARM")) {
                    i = new ItemStack(Material.POTION);
                    i.setDurability((short) 16428);
                } else {
                    i = new ItemStack(Material.valueOf(item));
                }
                inventoryOrder.put(i, slot);
            }
        }
    }

    public Boolean canChangeTalents() {
        return bg != null && ((plugin.getBG(bg) != null && !plugin.getBG(bg).matchStarted));
    }

    public void loadTalents(String talentsStr) {
        String section = "talents";
        ConfigFile config = new ConfigFile(plugin, "talents.yml");

        talentMaterials.clear();
        talentAbilities.clear();
        talents.clear();

        List<Integer> talentSlots = new ArrayList<>();
        if (talentsStr != null) {
            for (String talent : talentsStr.split(",")) {
                talentSlots.add(Integer.parseInt(talent));
            }
        }

        if (config.configFile.exists()) {
            for (String infoItem : config.getAll(section, null)) {
                String title = translateAlternateColorCodes('&',
                        config.get(section + "." + infoItem + ".title", null));

                Material material = Material.getMaterial(config.get(section + "." + infoItem + ".item", Material.PAPER));
                int slot = Integer.parseInt(config.get(section + "." + infoItem + ".slot", 0));
                int cooldown = Integer.parseInt(config.get(section + "." + infoItem + ".cooldown", 0));
                int length = Integer.parseInt(config.get(section + "." + infoItem + ".length", 0));

                if (talentSlots.contains(slot)) {
                    if (cooldown > 0) {
                        talentAbilities.put(Talent.valueOf(slot),
                                new TalentAbility(plugin, Talent.valueOf(slot), stripColor(title), material, length, cooldown));
                    }
                    talents.add(Talent.valueOf(slot));
                    talentMaterials.put(material, Talent.valueOf(slot));
                }
            }
        }

        if (talents.contains(Talent.POTION_SLINGER)) {
            maxHarmingPotions = 3;
        } else {
            maxHarmingPotions = 2;
        }
    }

    public String talentsToString() {
        StringBuilder b = new StringBuilder();
        for (Talent t : talents) {
            b.append("").append(Talent.toInt(t)).append(",");
        }
        return b.substring(0, b.length() - 1);
    }

    public void sendMessage(String message) {
        Player p = getPlayer();
        if (p != null && p.isOnline()) {
            p.sendMessage(message);
        }
    }

    public Location getLocation() {
        if (getPlayer() == null) return null;
        return getPlayer().getLocation();
    }

    public Player getPlayer() {
        return plugin.getServer().getPlayerExact(name);
    }

    public void cleanPlayer() {
        Player player = getPlayer();
        if (player == null) return;

        //Remove potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        //Remove arrows stuck in player
        ((CraftPlayer) getPlayer()).getHandle().getDataWatcher().watch(9, (byte) 0);

    }

    public void restorePlayer() {
        Player p = plugin.getServer().getPlayer(name);
        if (p != null && p.isOnline() && !p.isDead()) {
            //Send the player back to Loka
            plugin.bungee.sendPlayer(getPlayer());
        }
    }

    public void preparePlayer(Boolean battleground) {
        Player player = getPlayer();

        cleanPlayer();
        setPlayerGear(battleground);
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setExp(0);
        player.setLevel(0);
    }

    void setArmorMetas(ItemStack armorPiece) {
        LeatherArmorMeta lam = (LeatherArmorMeta) armorPiece.getItemMeta();
        lam.setColor(playerColor);
        armorPiece.setItemMeta(lam);
    }

    public void setPlayerGear(Boolean battleground) {
        Player p = getPlayer();

        if (p != null) {
            p.getInventory().clear();

            //Weapons
            ItemStack sword = new ItemStack(Material.STONE_SWORD);
            sword.addEnchantment(Enchantment.DURABILITY, 1);

            ItemStack bow = new ItemStack(Material.BOW);
            appendTalentsToBow(bow);

            ItemStack arrows = new ItemStack(Material.ARROW);
            arrows.setAmount(24);
            if (battleground) {
                arrows.setAmount(32);
            }

            //Armor
            setPlayerArmor(battleground);

            int numHealPotions = 1;
            if (battleground) numHealPotions = 2;
            if (talents.contains(Talent.POTENCY)) numHealPotions = 3;
            if (!battleground) maxHarmingPotions = 2;

            List<ItemStack> items = new ArrayList<>();

            //Add standard gearset to items
            items.add(sword);
            items.add(bow);
            items.add(new ItemStack(Material.POTION, numHealPotions, (short) 16389));
            items.add(new ItemStack(Material.POTION, maxHarmingPotions, (short) 16428));

            //Add ability items
            items.addAll(getAbilityItems());
            items.add(arrows);

            setInventory(items, p);
        }
    }

    void setInventory(List<ItemStack> items, Player p) {
        for (ItemStack item : items) {
            int slot = getOrder(item);
            if (slot >= 0) {
                if (p.getInventory().getItem(slot) != null) {
                    //There's an item already here for some reason, add to the inventory instead.
                    if (item.getType().equals(Material.ARROW)) {
                        p.getInventory().setItem(8, item);
                    } else {
                        p.getInventory().addItem(item);
                    }
                } else {
                    //Set it in the saved slot
                    p.getInventory().setItem(slot, item);
                }
            } else {
                //Else just add it normally.
                if (item.getType().equals(Material.ARROW)) {
                    p.getInventory().setItem(8, item);
                } else {
                    p.getInventory().addItem(item);
                }
            }
        }
    }

    public int getOrder(ItemStack item) {
        for (ItemStack i : inventoryOrder.keySet()) {
            if (i.getType().equals(item.getType())) {
                if (!inventoryOrder.containsKey(i)) break;

                //Special case for potions
                if (i.getType().equals(Material.POTION)) {
                    if (i.getDurability() == item.getDurability()) {
                        return inventoryOrder.get(i);
                    }
                } else {
                    return inventoryOrder.get(i);
                }
            }
        }
        return -1;
    }

    public void setPlayerArmor(Boolean battleground) {
        ItemStack helm = new ItemStack(Material.LEATHER_HELMET);
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);

        applyTalentsToChest(chest);
        applyTalentsToLeggings(leggings);
        applyTalentsToBoots(boots);

        //Set armor enchants + colors
        setArmorMetas(helm);
        setArmorMetas(boots);
        setArmorMetas(chest);
        setArmorMetas(leggings);

        //Protection
        if (hasTalent(Talent.HARDENED) || !battleground) {
            helm.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            leggings.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        } else {
            helm.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
            leggings.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
        }

        boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        chest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);

        //Unbreaking
        helm.addEnchantment(Enchantment.DURABILITY, 3);
        chest.addEnchantment(Enchantment.DURABILITY, 3);
        leggings.addEnchantment(Enchantment.DURABILITY, 3);
        boots.addEnchantment(Enchantment.DURABILITY, 3);

        Player p = getPlayer();
        p.getInventory().setHelmet(helm);
        p.getInventory().setBoots(boots);
        p.getInventory().setChestplate(chest);
        p.getInventory().setLeggings(leggings);
    }

    private List<ItemStack> getAbilityItems() {
        List<ItemStack> abilityItems = new ArrayList<>();

        for (TalentAbility t : talentAbilities.values()) {
            if (plugin.talents.itemlessTalents.contains(t.talent)) continue;

            ItemStack item = new ItemStack(t.material);

            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setDisplayName(t.name);
            itemMeta.setLore(plugin.talents.talentLore.get(t.talent));
            item.setItemMeta(itemMeta);

            //If it's on cooldown
            if (!t.available) {
                item.setType(Material.COAL);
            }

            abilityItems.add(item);
            t.item = item;
        }

        if (talents.contains(Talent.BOUNCY)) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 50000, 0));
        }

        if (talents.contains(Talent.FLEET_FOOTED)) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 50000, 0));
        }

        if (talents.contains(Talent.FRESH_START)) {
            getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 50000, 0));
        }
        return abilityItems;
    }

    void applyTalentsToChest(ItemStack chest) {
        if (talents.contains(Talent.THORNS)) {
            chest.addEnchantment(Enchantment.THORNS, 3);
        }

        if (talents.contains(Talent.FIRE_PROTECTION)) {
            chest.addEnchantment(Enchantment.PROTECTION_FIRE, 1);
        }

        if (talents.contains(Talent.BLAST_PROTECTION)) {
            chest.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 1);
        }

        if (talents.contains(Talent.HARDENED)) {
            chest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        } else {
            chest.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
        }
    }

    void applyTalentsToLeggings(ItemStack leggings) {
        if (talents.contains(Talent.THORNS)) {
            leggings.addEnchantment(Enchantment.THORNS, 3);
        }
    }

    void applyTalentsToBoots(ItemStack boots) {
        if (talents.contains(Talent.BOUNCY)) {
            boots.addEnchantment(Enchantment.PROTECTION_FALL, 2);
        }

        if (talents.contains(Talent.THORNS)) {
            boots.addEnchantment(Enchantment.THORNS, 3);
        }

        if (talents.contains(Talent.HARDENED)) {
            boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        } else {
            boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
        }
    }

    void appendTalentsToBow(ItemStack bow) {
        if (talents.contains(Talent.ARCHER)) {
            bow.addEnchantment(Enchantment.ARROW_DAMAGE, 2);
        }
    }

    public void activateTalent(Talent t) {
        if (talentAbilities.containsKey(t)) {
            talentAbilities.get(t).activate(this, getPlayer().getItemInHand());
        }
    }

    public void eggPlayer() {
        Player player = getPlayer();
        if (player == null) return;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 180, 5));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 180, 3));
        player.getInventory().clear();
        ItemStack eggs = new ItemStack(Material.EGG);
        eggs.setAmount(64);
        player.getInventory().addItem(eggs);
        player.setHealth(20);
    }

    Integer getVotaScore() {
        return valleyCaps * 3 + valleyKills - valleyDeaths;
    }

    public boolean hasTalent(Talent t) {
        return !(talents == null || talents.size() == 0) && talents.contains(t);
    }

    public boolean talentAvailable(Talent t) {
        return !(talents == null || talents.size() == 0) && talents.contains(t) && talentAbilities.get(t).available;
    }

    public boolean hasTalentActive(Talent t) {
        return talentAbilities.containsKey(t) && talentAbilities.get(t).active;
    }

    public void updateBGRating(Double ratingChange, Integer score) {
        if (ratingChange == null || score == null) return;

        bgRating += (ratingChange) + (score / 3);
        log.info(name + " - BGRating change: " + ratingChange + " + " + (score / 3) + " = " + ((bgRating) + (score / 3))
                + ". Now rated: " + bgRating);
    }
}
