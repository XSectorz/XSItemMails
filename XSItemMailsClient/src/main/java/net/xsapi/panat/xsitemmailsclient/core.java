package net.xsapi.panat.xsitemmailsclient;

import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsclient.utils.XSUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class core extends JavaPlugin {


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
        XSHandler.closeAllOpenInventory();
    }
}
