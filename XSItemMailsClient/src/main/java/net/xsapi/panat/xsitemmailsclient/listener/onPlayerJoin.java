package net.xsapi.panat.xsitemmailsclient.listener;

import net.xsapi.panat.xsitemmailsclient.core;
import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsclient.redis.XS_REDIS_MESSAGES;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class onPlayerJoin implements Listener {

    @EventHandler
    public void onPlayerJoinServer(PlayerJoinEvent e) {

        if(!XSHandler.getPlayerDataReference().containsKey(e.getPlayer().getName())) {
            core.getPlugin().getLogger().info("SENT " + e.getPlayer().getName());
            //XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(), XS_REDIS_MESSAGES.REQUEST_PLAYER_DATA_TO_SERVER_SPECIFIC+"<SPLIT>"+XSHandler.getServerClient()+";"+e.getPlayer());
        }

    }
}
