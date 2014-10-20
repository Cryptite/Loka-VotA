package com.cryptite.pvp.vota;

import com.cryptite.pvp.PvPPlayer;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scoreboard.Score;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.cryptite.pvp.utils.LocationUtils.inWGRegion;


class VotAArtifact {
    private final VotA vota;
    private Score scoreboardObjective;
    public final String name;
    public Boolean contested = false;
    public Boolean controlled = false;
    public String controllingTeam;
    public String capturingTeam;
    public ChatColor teamColor = ChatColor.GRAY;
    public Integer progress = 0;
    public String region;
    public Integer woolMinX = 0;
    public Integer woolMinZ = 0;
    public Integer woolMaxX = 0;
    public Integer woolMaxZ = 0;
    public Integer woolY = 0;
    public World world;
    public final List<Block> artifactBlocks = new ArrayList<>();
    public Location fireworkSource;

    public VotAArtifact(VotA vota, String name) {
        this.vota = vota;
        this.name = name;
    }

    void launchFirework(Color color) {
        Firework fw = world.spawn(fireworkSource, Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();

        //Create our effect with this
        FireworkEffect effect = FireworkEffect.builder().withColor(color).with(Type.BALL).build();

        //Then apply the effect to the meta
        fwm.addEffects(effect);
        fwm.setPower(2);

        //Then apply this to our rocket
        fw.setFireworkMeta(fwm);
    }

    void setControlledWoolColor(String team) {
        if (team == null) {
            teamColor = ChatColor.GRAY;
        } else if (team.equals("blue")) {
            launchFirework(Color.BLUE);
            teamColor = ChatColor.BLUE;
        } else if (team.equals("red")) {
            launchFirework(Color.RED);
            teamColor = ChatColor.RED;
        }

        for (int x = woolMinX; x <= woolMaxX; x++) {
            for (int z = woolMinZ; z <= woolMaxZ; z++) {
                Block block = world.getBlockAt(x, woolY, z);
                if (block.getType() == Material.WOOL) {
                    block.setTypeIdAndData(35, getWoolColorData(team), true);
                }
            }
        }
        vota.updateArtifactControlObjectives();
    }

    byte getWoolColorData(String team) {
        DyeColor color;

        //If the team is null, try the capturing team
        if (team == null) {
            team = capturingTeam;
        }

        if (team == null) {
            color = DyeColor.GRAY;
        } else if (team.equals("blue")) {
            color = DyeColor.BLUE;
        } else {
            color = DyeColor.RED;
        }
        return color.getData();
    }

    void setVisualProgress() {
        if (capturingTeam == null) return;

        if (progress > 0 && progress < 30) {
            int count = (int) (progress / 3.75);
            List<Block> changedBlocks = new ArrayList<>();
            for (Block b : artifactBlocks) {
                b.setTypeIdAndData(35, getWoolColorData(capturingTeam), true);
                changedBlocks.add(b);
                count--;
                if (count <= 0) break;
            }
            for (Block b : artifactBlocks) {
                if (!changedBlocks.contains(b)) {
                    b.setType(Material.OBSIDIAN);
                }
            }
        } else {
            if (controlled && controllingTeam != null) {
                for (Block b : artifactBlocks) b.setTypeIdAndData(35, getWoolColorData(controllingTeam), true);
            } else {
                for (Block b : artifactBlocks) b.setType(Material.OBSIDIAN);
            }
        }
    }

    public Boolean isContested(String team) {
        //Controlling team owns the point
        if (controllingTeam != null && !controllingTeam.equals(capturingTeam)) return true;

        //Capturing team has progress on taking the artifact
        return capturingTeam != null && !capturingTeam.equals(team);
    }

    public void setControlled(String team) {
        controlled = true;
        setControlledWoolColor(team);
        controllingTeam = team;
        setVisualProgress();
    }

    public void setNeutral() {
        capturingTeam = null;
        controlled = false;
        controllingTeam = null;
        setControlledWoolColor(null);
        setVisualProgress();
    }

    public List<PvPPlayer> getCapturers(Collection<PvPPlayer> players) {
        List<PvPPlayer> cappers = new ArrayList<>();

        for (PvPPlayer p : players) {
            if (inWGRegion(region, p.name)
                    && p.getPlayer() != null
                    && !p.getPlayer().isDead()) {
                cappers.add(p);
            }
        }

        return cappers;
    }

    public Boolean isCapturable(String team) {
        return capturingTeam == null || capturingTeam.equals(team);
    }
}