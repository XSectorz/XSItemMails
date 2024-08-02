package net.xsapi.panat.xsitemmailsserver;

import net.md_5.bungee.api.plugin.Plugin;
import net.xsapi.panat.xsitemmailsserver.database.XSDatabaseHandler;
import net.xsapi.panat.xsitemmailsserver.handler.XSHandler;
import net.xsapi.panat.xsitemmailsserver.redis.XSRedisHandler;

public final class core extends Plugin {

    private static core plugin;

    public static core getPlugin() {
        return plugin;



    }

    @Override
    public void onEnable() {
        plugin = this;

        XSHandler.initSystem();

    }

    @Override
    public void onDisable() {

        XSRedisHandler.destroyThreads();
        XSDatabaseHandler.saveDataToSQL();
    }
}
