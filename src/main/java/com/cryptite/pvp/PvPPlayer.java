package com.cryptite.pvp;

import com.cryptite.pvp.data.Town;
import com.cryptite.pvp.db.DBData;
import com.cryptite.pvp.rest.Rest;
import com.cryptite.pvp.rest.RestPlayer;
import com.cryptite.pvp.talents.Aura;
import com.cryptite.pvp.talents.Bandage;
import com.cryptite.pvp.talents.Talent;
import com.cryptite.pvp.talents.TalentAbility;
import com.cryptite.pvp.utils.Rope;
import com.mongodb.BasicDBObject;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.logging.Logger;

import static org.bukkit.ChatColor.stripColor;
import static org.bukkit.ChatColor.translateAlternateColorCodes;

public class PvPPlayer {
    private final Logger log = Logger.getLogger("Artifact-PvPPlayer");
    private final LokaVotA plugin;

    public final String name;
    public UUID uuid;

    public Location location;
    public Town town;
    public String bg;
    public double damage = 0;
    public String title;
    public String rank;
    public Boolean ready = false;
    public Boolean isAlive = true;
    public Boolean spectator = false;
    public Color playerColor;
    public List<Talent> talents = new ArrayList<>();
    public boolean talentsSaved = false;

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

    //Talents
    public final Map<Talent, TalentAbility> talentAbilities = new HashMap<>();
    public final Map<Material, Talent> talentMaterials = new HashMap<>();
    public Map<String, Integer> inventoryOrder = new HashMap<>();
    public Rope hook;
    public Bandage bandage;
    public Aura aura;

    //Talent-Groove
    public int grooveStacks = 0;
    public int maxHarmingPotions = 2;
    public boolean offense;
    public String team;

    //Achievement trackers
    public final List<String> lastStandAttackers = new ArrayList<>();

    public PvPPlayer(LokaVotA plugin, String name) {
        this.name = name;
        this.plugin = plugin;
    }

    public void update(BasicDBObject data) {
        new DBData(this).update(data);
    }

    public void update(String key, Object value) {
        new DBData(this).set(key, value);
    }

    public void increment(String key) {
        increment(key, 1);
    }

    public void increment(String key, int amount) {
        new DBData(this).increment(key, amount);
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
        if (name == null) return;

        DBData dbData = new DBData(this);
        uuid = dbData.uuid;

        rank = dbData.get("rank", rank);
//        prowess = Integer.parseInt(dbData.get("prowess", prowess));
        arrowHits = Integer.parseInt(dbData.get("arrowhits", arrowHits));
        arrowsFired = Integer.parseInt(dbData.get("arrowsfired", arrowsFired));
        valleyKills = Integer.parseInt(dbData.get("valleyKills", valleyKills));
        valleyDeaths = Integer.parseInt(dbData.get("valleyDeaths", valleyDeaths));
        valleyCaps = Integer.parseInt(dbData.get("valleyCaps", valleyCaps));
        valleyWins = Integer.parseInt(dbData.get("valleyWins", valleyWins));
        valleyLosses = Integer.parseInt(dbData.get("valleyLosses", valleyLosses));
        bgRating = Integer.parseInt(dbData.get("bgRating", bgRating));
        valleyScore = getVotaScore();
        loadTalents(dbData.get("talents", null));

        String town = dbData.get("town", null);
        if (town != null) this.town = plugin.getTown(town);

        if (dbData.data != null) {
            Map<String, Integer> inventoryData = (Map) dbData.data.get("inventoryorder");
            if (inventoryData != null) inventoryOrder.putAll(inventoryData);
        }

        if (valleyWins == 0 || valleyLosses == 0) updateMissingPvPData();
    }

    private void updateMissingPvPData() {
        ConfigFile config = new ConfigFile(plugin, "players/" + name + ".yml");
        update("valleyKills", Integer.parseInt(config.get("valleyKills", valleyKills)));
        update("valleyDeaths", Integer.parseInt(config.get("valleyDeaths", valleyDeaths)));
        update("valleyCaps", Integer.parseInt(config.get("valleyCaps", valleyCaps)));
        System.out.println("[DB] Updated missing data for " + name);
    }

    public Boolean isTownOwner() {
        return town != null && town.owner.equals(name);
    }

    public Boolean canChangeTalents() {
        return bg != null && ((plugin.getBG(bg) != null && !plugin.getBG(bg).matchStarted));
    }

    public void loadTalents(String talentsStr) {
        if (talentsStr == null) return;

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

                //Slot 99 is the bandage
                if (slot == 99 || talentSlots.contains(slot)) {
                    if (cooldown > 0) {
                        talentAbilities.put(Talent.valueOf(slot),
                                new TalentAbility(plugin, Talent.valueOf(slot), stripColor(title), material, length, cooldown));
                    }
                    talents.add(Talent.valueOf(slot));
                    talentMaterials.put(material, Talent.valueOf(slot));
                }
            }
        }

        //Improved bandage makes Bandage's length 9 seconds
        if (talents.contains(Talent.IMPROVED_BANDAGE) && talentAbilities.containsKey(Talent.BANDAGE)) {
            talentAbilities.get(Talent.BANDAGE).length = 9;
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

    public String getTownRank() {
        if (town == null) return "";
        return town.getRank(name);
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
            if (bg != null && bg.equals("Skyvale")) {
                items.add(new ItemStack(Material.STONE_PICKAXE));
            }

            //Add ability items
            items.addAll(getAbilityItems());
            items.add(arrows);

            setInventory(items, p);

            //Start Aura
            if (!hasTalent(Talent.SILENCE)) return;

            if (aura != null) {
                aura.cancel();
                aura = new Aura(this);
            } else {
                aura = new Aura(this);
            }
        }
    }

    void setInventory(List<ItemStack> items, Player p) {
        for (ItemStack item : items) {
            if (item == null) continue;

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

    public void saveInventoryOrder() {
        inventoryOrder.clear();
        for (int i = 0; i <= getPlayer().getInventory().getSize(); i++) {
            ItemStack item = getPlayer().getInventory().getItem(i);
            if (item != null && !item.getType().equals(Material.COAL)) {
                if (item.getType().equals(Material.POTION)) {
                    if (item.getDurability() == 16389) {
                        inventoryOrder.put("POTION-HEAL", i);
                    } else {
                        inventoryOrder.put("POTION-HARM", i);
                    }
                } else {
                    inventoryOrder.put(item.getType().toString(), i);
                }
            }
        }

        update("inventoryorder", new BasicDBObject(inventoryOrder));
    }

    public int getOrder(ItemStack item) {
        for (String i : inventoryOrder.keySet()) {
            if (i.equals(item.getType().toString())) {
                if (!inventoryOrder.containsKey(i)) break;

                //Special case for potions
                if (i.equals(Material.POTION.toString())) {
                    if (item.getDurability() == 16389) {
                        return inventoryOrder.get("POTION-HEAL");
                    } else {
                        return inventoryOrder.get("POTION-HARM");
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

    void applyTalentsToBoots(ItemStack boots) {
        if (talents.contains(Talent.BOUNCY)) {
            boots.addEnchantment(Enchantment.PROTECTION_FALL, 2);
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
