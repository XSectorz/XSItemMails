package net.xsapi.panat.xsitemmailsserver.config;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.xsapi.panat.xsitemmailsserver.core;

import java.io.*;

public class mainConfig {

    private static Configuration configuration;

    public static Configuration getConfig() {
        return configuration;
    }

    public mainConfig() throws IOException {
        if (!core.getPlugin().getDataFolder().exists()) {
            core.getPlugin().getDataFolder().mkdir();
        }

        File file = new File(core.getPlugin().getDataFolder(), "config.yml");

        if (!file.exists()) {
            file.createNewFile();
            try (InputStream in = core.getPlugin().getResourceAsStream("config.yml");
                 OutputStream out = new FileOutputStream(file)) {
                ByteStreams.copy(in, out);
            }
        }

        configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
    }

}
