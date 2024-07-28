package net.xsapi.panat.xsitemmailsserver.listeners;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.xsapi.panat.xsitemmailsserver.database.XSDatabaseHandler;

public class onPlayerJoin implements Listener {

    @EventHandler
    public void onJoin(PostLoginEvent e) {
        ProxiedPlayer p = e.getPlayer();
        XSDatabaseHandler.createUserSQL(p);

    }

}
