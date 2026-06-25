package com.davud.makeachild.commands;

import com.davud.makeachild.MakeAChild;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class MakeAChildCommand implements CommandExecutor {

    private final MakeAChild plugin;

    public MakeAChildCommand(MakeAChild plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("biome")) {
            if (!sender.hasPermission("makeachild.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }
            
            String newTypeStr = args[1].toUpperCase();
            try {
                Villager.Type.valueOf(newTypeStr);
                plugin.getConfig().set("villager-type", newTypeStr);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.GREEN + "Villager biome type set to " + newTypeStr);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid biome type. Valid types are: DESERT, JUNGLE, PLAINS, SAVANNA, SNOW, SWAMP, TAIGA");
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        
        long lastUsed = plugin.getConfig().getLong("cooldowns." + player.getUniqueId().toString(), 0L);
        long cooldownMillis = 24 * 60 * 60 * 1000L;
        if (System.currentTimeMillis() - lastUsed < cooldownMillis && !player.hasPermission("makeachild.admin")) {
            long remaining = cooldownMillis - (System.currentTimeMillis() - lastUsed);
            long remainingHours = remaining / (60 * 60 * 1000L);
            long remainingMinutes = (remaining % (60 * 60 * 1000L)) / (60 * 1000L);
            player.sendMessage(ChatColor.RED + "Bu komutu tekrar kullanabilmek için " + remainingHours + " saat " + remainingMinutes + " dakika beklemelisin.");
            return true;
        }
        
        // Find nearest villager within 2 blocks
        Villager targetVillager = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Entity entity : player.getNearbyEntities(2.0, 2.0, 2.0)) {
            if (entity instanceof Villager) {
                double distance = entity.getLocation().distance(player.getLocation());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    targetVillager = (Villager) entity;
                }
            }
        }
        
        if (targetVillager == null) {
            player.sendMessage(ChatColor.RED + "Yakınında (2 blok) hiç köylü yok!");
            return true;
        }
        
        // Spawn heart particles
        spawnHearts(player.getLocation());
        spawnHearts(targetVillager.getLocation());
        
        plugin.getConfig().set("cooldowns." + player.getUniqueId().toString(), System.currentTimeMillis());
        plugin.saveConfig();
        
        List<String> males = plugin.getConfig().getStringList("males");
        boolean isMale = males.contains(player.getName());
        
        String typeStr = plugin.getConfig().getString("villager-type", "PLAINS").toUpperCase();
        Villager.Type vType = Villager.Type.PLAINS;
        try {
            vType = Villager.Type.valueOf(typeStr);
        } catch (Exception e) {
            // Ignored, fallback to PLAINS
        }
        final Villager.Type finalVType = vType;
        
        if (isMale) {
            // Male logic: baby spawns from villager immediately
            spawnBaby(targetVillager.getLocation(), finalVType);
            player.sendMessage(ChatColor.GREEN + "Bir çocuğunuz oldu!");
        } else {
            // Female logic
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Tebrikler Hamilesin");
            
            // 1 min = 1200 ticks
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.YELLOW + "Hamilelik belirtileri başladı...");
                        // Add effects for 1 min (1200 ticks)
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1200, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 1200, 1));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 1200, 1));
                    }
                }
            }.runTaskLater(plugin, 1200L);
            
            // another 1 min = 2400 ticks total
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        spawnBaby(player.getLocation(), finalVType);
                        player.sendMessage(ChatColor.GREEN + "Tebrikler! Bebeğiniz doğdu.");
                    }
                }
            }.runTaskLater(plugin, 2400L);
        }
        
        return true;
    }
    
    private void spawnHearts(Location loc) {
        loc.getWorld().spawnParticle(Particle.HEART, loc.clone().add(0, 2, 0), 10, 0.5, 0.5, 0.5);
    }
    
    private void spawnBaby(Location loc, Villager.Type type) {
        Villager baby = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        baby.setBaby();
        baby.setVillagerType(type);
    }
}
