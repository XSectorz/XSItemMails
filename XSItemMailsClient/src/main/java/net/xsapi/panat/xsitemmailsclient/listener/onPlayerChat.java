package net.xsapi.panat.xsitemmailsclient.listener;

import net.xsapi.panat.xsitemmailsclient.config.XS_MENU_FILE;
import net.xsapi.panat.xsitemmailsclient.config.mainConfig;
import net.xsapi.panat.xsitemmailsclient.config.menuConfig;
import net.xsapi.panat.xsitemmailsclient.core;
import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.handler.XS_ITEMS_EDITOR_TOPICS;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsclient.redis.XS_REDIS_MESSAGES;
import net.xsapi.panat.xsitemmailsclient.utils.XSUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class onPlayerChat implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        String message = ChatColor.stripColor(e.getMessage());
        Player p = e.getPlayer();

        if(XSHandler.getPlayerCreatorTopics().containsKey(p)) {

            e.setCancelled(true);
            XS_ITEMS_EDITOR_TOPICS topics = XSHandler.getPlayerCreatorTopics().get(p);

            if(topics.equals(XS_ITEMS_EDITOR_TOPICS.INPUT_NAME)) {
                String itemNames = ChatColor.stripColor(message);
                XSHandler.getPlayerCreatorTopics().remove(p);
                if(XSHandler.getXsItemmailsHashMap().containsKey(itemNames)) {
                    XSUtils.sendMessageFromConfig("create_fail",p);
                    return;
                }

                String base64Items = XSUtils.itemStackToBase64(XSUtils.decodeItemFromConfig("settings.default_preview_items",mainConfig.getConfig(),p.getName(),null));

                XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(),
                        XS_REDIS_MESSAGES.CREATE_ITEM+"<SPLIT>" + itemNames + ";" + XSHandler.getServerClient() + ";" + p.getName() + ";" + base64Items);
            } else if(topics.equals(XS_ITEMS_EDITOR_TOPICS.INPUT_COMMAND)) {
                String type = message.split(" ")[0];

                if(type.equalsIgnoreCase("[CONSOLE]") || type.equalsIgnoreCase("[PLAYER]")) {
                    XSHandler.getPlayerCreatorTopics().remove(p);
                    XSUtils.sendMessageFromConfig("add_command_success",p);
                    XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(), XS_REDIS_MESSAGES.UPDATE_DATA_TO_SERVER+"<SPLIT>"+"add_commands;"
                            +XSHandler.getServerClient()+";"+XSHandler.getPlayerEditorKey().get(p)+";"+message+";"+p.getName());

                } else {
                    XSUtils.sendMessageFromConfig("type_format_not_accept",p);
                }

            }


        }

    }
}
