package net.xsapi.panat.xsitemmailsclient.listener;

import net.xsapi.panat.xsitemmailsclient.config.XS_MENU_FILE;
import net.xsapi.panat.xsitemmailsclient.config.menuConfig;
import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.utils.XSUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

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

                } else if(key.equalsIgnoreCase("back_button")) {
                    if(XSHandler.getPlayerPage().get(p) > 1) {
                        XSHandler.getPlayerPage().put(p,XSHandler.getPlayerPage().get(p)-1);
                    }
                } else if(key.equalsIgnoreCase("create")) {

                }

            }

            e.setCancelled(true);
        }

    }
}
