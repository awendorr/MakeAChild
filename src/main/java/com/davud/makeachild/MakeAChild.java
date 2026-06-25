package com.davud.makeachild;

import com.davud.makeachild.commands.MakeAChildCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MakeAChild extends JavaPlugin {

    private static MakeAChild instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        getCommand("makeachild").setExecutor(new MakeAChildCommand(this));
        
        getLogger().info("MakeAChild plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("MakeAChild plugin disabled!");
    }

    public static MakeAChild getInstance() {
        return instance;
    }
}
