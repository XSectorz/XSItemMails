package net.xsapi.panat.xsitemmailsclient.config;

import net.xsapi.panat.xsitemmailsclient.core;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class menuConfig {

    public static HashMap<String,File> customConfigFile = new HashMap<>();

    public static HashMap<String,FileConfiguration> customConfig = new HashMap<>();

    public static FileConfiguration getConfig(XS_MENU_FILE xsFile) {
        return customConfig.get(xsFile.toString().toLowerCase());
    }

    public void loadConfigu(XS_MENU_FILE XSFile) {

        String file = XSFile.toString().toLowerCase();
        customConfigFile.put(file,new File(core.getPlugin().getDataFolder(), "menu/"+file+".yml"));
        if (!customConfigFile.get(file).exists()) {
            customConfigFile.get(file).getParentFile().mkdirs();
            core.getPlugin().saveResource("menu/"+file+".yml", false);
        }
        customConfig.put(file, new YamlConfiguration());
        try {
            customConfig.get(file).load(customConfigFile.get(file));
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void save(XS_MENU_FILE XSFile) {

        String file = XSFile.toString().toLowerCase();
        customConfigFile.put(file,new File(core.getPlugin().getDataFolder(), "menu/"+file+".yml"));
        try {
            customConfig.get(file).options().copyDefaults(true);
            customConfig.get(file).save(customConfigFile.get(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reload(XS_MENU_FILE XSFile) {

        String file = XSFile.toString().toLowerCase();
        customConfigFile.put(file,new File(core.getPlugin().getDataFolder(), "menu/"+file+".yml"));
        if(!customConfigFile.get(file).exists()) {
            customConfigFile.get(file).getParentFile().mkdirs();
            core.getPlugin().saveResource("menu/"+file+".yml", false);
        } else {
            customConfig.put(file, YamlConfiguration.loadConfiguration(customConfigFile.get(file)));
            try {
                customConfig.get(file).save(customConfigFile.get(file));
                core.getPlugin().reloadConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
