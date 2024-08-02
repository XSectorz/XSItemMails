package net.xsapi.panat.xsitemmailsclient.listener;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.xsapi.panat.xsitemmailsclient.config.XS_MENU_FILE;
import net.xsapi.panat.xsitemmailsclient.config.menuConfig;
import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.handler.XS_ITEMS_EDITOR_TOPICS;
import net.xsapi.panat.xsitemmailsclient.objects.XSItemmails;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsclient.redis.XS_REDIS_MESSAGES;
import net.xsapi.panat.xsitemmailsclient.utils.XSUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class onInventory implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent e) {

        Player p = (Player) e.getWhoClicked();

        if(e.getView().getTitle().equalsIgnoreCase(XSUtils.decodeText(menuConfig.getConfig(XS_MENU_FILE.XS_MAIN_MENU).getString("settings.title")))) {

            int slot = e.getSlot();

            if(XSHandler.getPlayerGUISection().get(p).containsKey(slot)) {
                String key = XSHandler.getPlayerGUISection().get(p).get(slot);

                if (key.equalsIgnoreCase("close")) {
                    p.closeInventory();
                } else if(key.equalsIgnoreCase("next_button")) {
                    XSHandler.getPlayerPage().put(p,XSHandler.getPlayerPage().get(p)+1);
                    XSUtils.updateInventoryContent(menuConfig.getConfig(XS_MENU_FILE.XS_MAIN_MENU),p,null);
                } else if(key.equalsIgnoreCase("back_button")) {
                    if(XSHandler.getPlayerPage().get(p) > 1) {
                        XSHandler.getPlayerPage().put(p,XSHandler.getPlayerPage().get(p)-1);
                        XSUtils.updateInventoryContent(menuConfig.getConfig(XS_MENU_FILE.XS_MAIN_MENU),p,null);
                    }
                } else if(key.equalsIgnoreCase("create")) {
                    //p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),p));
                    XSHandler.getPlayerCreatorTopics().put(p, XS_ITEMS_EDITOR_TOPICS.INPUT_NAME);
                    p.closeInventory();
                    XSUtils.sendMessageFromConfig("input_name",p);
                } else {
                    if(XSHandler.getXsItemmailsHashMap().containsKey(key)) {
                        XSHandler.getPlayerEditorKey().put(p,key);
                        XSHandler.getPlayerPage().put(p,1);
                        p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),p,XSHandler.getXsItemmailsHashMap().get(key)));
                    }
                }

            }

            e.setCancelled(true);
        } else if(e.getView().getTitle().equalsIgnoreCase(XSUtils.decodeText(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE).getString("settings.title")))) {
            int slot = e.getSlot();
            if(e.getClickedInventory().equals(e.getView().getBottomInventory())) {
                p.sendMessage("Bottom Inv");
            } else {
                e.setCancelled(true);

                if(XSHandler.getPlayerGUISection().get(p).containsKey(slot)) {
                    p.sendMessage("Top Inv");
                    String key = XSHandler.getPlayerGUISection().get(p).get(slot);
                    p.sendMessage(key);
                    if (key.equalsIgnoreCase("back_to_main_menu")) {
                        XSHandler.getPlayerPage().put(p,1);
                        p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_MAIN_MENU),p,null));
                    } else if (key.equalsIgnoreCase("preview_display_items")) {
                        ItemStack it = e.getCursor();

                        if(it != null) {
                            p.sendMessage(it.getType().toString());
                            String itBase64 = XSUtils.itemStackToBase64(it);

                            XSItemmails xsItemmails = XSHandler.getXsItemmailsHashMap().get(XSHandler.getPlayerEditorKey().get(p));
                            xsItemmails.setItemDisplay(itBase64);

                            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(), XS_REDIS_MESSAGES.UPDATE_DATA_TO_SERVER+"<SPLIT>"+XSHandler.getPlayerEditorKey().get(p)
                            +";"+itBase64+";"+XSHandler.getServerClient()+";preview");
                            XSUtils.updateInventoryContent(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),p,xsItemmails);
                            e.setCursor(new ItemStack(Material.AIR));
                        }
                    }
                }
            }
        }

    }
}
