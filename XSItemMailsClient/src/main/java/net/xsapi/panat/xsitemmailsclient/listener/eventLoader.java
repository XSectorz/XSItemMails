package net.xsapi.panat.xsitemmailsclient.listener;

import net.xsapi.panat.xsitemmailsclient.core;
import org.bukkit.Bukkit;

public class eventLoader {

    public eventLoader() {

        Bukkit.getPluginManager().registerEvents(new onInventory(), core.getPlugin());
        Bukkit.getPluginManager().registerEvents(new onPlayerChat(), core.getPlugin());

    }

}
