package net.xsapi.panat.xsitemmailsclient.listener;

import com.google.gson.Gson;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.xsapi.panat.xsitemmailsclient.config.XS_MENU_FILE;
import net.xsapi.panat.xsitemmailsclient.config.menuConfig;
import net.xsapi.panat.xsitemmailsclient.config.messagesConfig;
import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.handler.XS_ITEMS_EDITOR_TOPICS;
import net.xsapi.panat.xsitemmailsclient.objects.XSItemmails;
import net.xsapi.panat.xsitemmailsclient.objects.XSRewards;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsclient.redis.XS_REDIS_MESSAGES;
import net.xsapi.panat.xsitemmailsclient.utils.XSUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class onInventory implements Listener {

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();

        if(e.getView().getTitle().equalsIgnoreCase(XSUtils.decodeText(menuConfig.getConfig(XS_MENU_FILE.XS_REWARD_ITEMS).getString("settings.title")))) {

            Inventory inv = e.getInventory();

            XSItemmails xsItemmails = XSHandler.getXsItemmailsHashMap().get(XSHandler.getPlayerEditorKey().get(p));
            ArrayList<String> itemRewardsList = xsItemmails.getRewardItems();
            ArrayList<String> tempRewardList = new ArrayList<>();

            for(String slotStr : menuConfig.getConfig(XS_MENU_FILE.XS_REWARD_ITEMS).getStringList("settings.additional_info.items_slot")) {

                int slot = Integer.parseInt(slotStr);

                ItemStack it = inv.getItem(slot);

                if(it != null && !it.getType().equals(Material.AIR))  {
                    //Bukkit.broadcastMessage(inv.getItem(slot).getType().toString());
                    tempRewardList.add(XSUtils.itemStackToBase64(it));

                }

            }

            int size = tempRewardList.size();

            //Bukkit.broadcastMessage("tempRewardList size: " + size);

            for(String dataItem : itemRewardsList) {
                if(tempRewardList.contains(dataItem)) {
                    size--;
                }
            }

            //Bukkit.broadcastMessage("size : " + size);

            if(size != 0 || tempRewardList.isEmpty()) { //something change!
                xsItemmails.setRewardItems(tempRewardList);

                Gson gson = new Gson();
                String dataJSON = gson.toJson(tempRewardList);
                //send to server to update
                XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(), XS_REDIS_MESSAGES.UPDATE_DATA_TO_SERVER+"<SPLIT>"+"item_rewards;"
                        +XSHandler.getServerClient()+";"+XSHandler.getPlayerEditorKey().get(p)+";"+dataJSON);
            }

        }

    }


    @EventHandler
    public void onClick(InventoryClickEvent e) {

        Player p = (Player) e.getWhoClicked();

        if(e.getClickedInventory() == null) {
            return;
        }

        if(!e.getClickedInventory().equals(e.getView().getBottomInventory()) && e.getView().getTitle().equalsIgnoreCase(XSUtils.decodeText(menuConfig.getConfig(XS_MENU_FILE.XS_MAIN_MENU).getString("settings.title")))) {

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

                        if(e.getClick().equals(ClickType.LEFT)) {
                            XSHandler.getPlayerEditorKey().put(p,key);
                            XSHandler.getPlayerPage().put(p,1);
                            p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),p,XSHandler.getXsItemmailsHashMap().get(key)));
                        } else if(e.getClick().equals(ClickType.DROP)) {

                            XSItemmails xsItemmails = XSHandler.getXsItemmailsHashMap().get(key);
                            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(),XS_REDIS_MESSAGES.DELETE_ITEM_TO_SERVER+"<SPLIT>"+xsItemmails.getId()+";"+key+";"+XSHandler.getServerClient()+";"+p.getName());
                        }
                    }
                }

            }

            e.setCancelled(true);
        } else if(e.getView().getTitle().equalsIgnoreCase(XSUtils.decodeText(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE).getString("settings.title")))) {
            int slot = e.getSlot();
            if(e.getClickedInventory().equals(e.getView().getBottomInventory())) {
            } else {
                e.setCancelled(true);

                if(XSHandler.getPlayerGUISection().get(p).containsKey(slot)) {
                    String key = XSHandler.getPlayerGUISection().get(p).get(slot);
                    if (key.equalsIgnoreCase("back_to_main_menu")) {
                        XSHandler.getPlayerPage().put(p,1);
                        p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_MAIN_MENU),p,null));
                    } else if (key.equalsIgnoreCase("preview_display_items")) {
                        ItemStack it = e.getCursor();

                        if(it != null && !it.getType().equals(Material.AIR)) {
                            p.sendMessage(it.getType().toString());
                            String itBase64 = XSUtils.itemStackToBase64(it);

                            XSItemmails xsItemmails = XSHandler.getXsItemmailsHashMap().get(XSHandler.getPlayerEditorKey().get(p));
                            xsItemmails.setItemDisplay(itBase64);

                            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(), XS_REDIS_MESSAGES.UPDATE_DATA_TO_SERVER+"<SPLIT>"+"preview;"
                                    +XSHandler.getServerClient()+";"+XSHandler.getPlayerEditorKey().get(p)+";"+itBase64);
                            XSUtils.updateInventoryContent(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),p,xsItemmails);
                            e.setCursor(new ItemStack(Material.AIR));
                        }
                    } else if (key.equalsIgnoreCase("items_reward")) {
                        XSItemmails xsItemmails = XSHandler.getXsItemmailsHashMap().get(XSHandler.getPlayerEditorKey().get(p));
                        p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_REWARD_ITEMS),p,xsItemmails));
                    } else if(key.equalsIgnoreCase("commands_reward")) {
                        if(e.getClick().equals(ClickType.LEFT)) {
                            XSHandler.getPlayerCreatorTopics().put(p, XS_ITEMS_EDITOR_TOPICS.INPUT_COMMAND);
                            p.closeInventory();
                            XSUtils.sendMessageFromConfig("input_command",p);
                        } else if(e.getClick().equals(ClickType.RIGHT)) {

                            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(), XS_REDIS_MESSAGES.UPDATE_DATA_TO_SERVER+"<SPLIT>"+"remove_commands;"
                                    +XSHandler.getServerClient()+";"+XSHandler.getPlayerEditorKey().get(p)+";"+p.getName());
                        }
                    }
                }
            }
        } else if(e.getView().getTitle().equalsIgnoreCase(XSUtils.decodeText(menuConfig.getConfig(XS_MENU_FILE.XS_REWARD_ITEMS).getString("settings.title")))) {
            int slot = e.getSlot();
            if(e.getClickedInventory().equals(e.getView().getBottomInventory())) {

            } else {

                if(!menuConfig.getConfig(XS_MENU_FILE.XS_REWARD_ITEMS).getStringList("settings.additional_info.items_slot").contains(String.valueOf(e.getSlot()))) {
                    e.setCancelled(true);
                }
                if(XSHandler.getPlayerGUISection().get(p).containsKey(slot)) {
                    String key = XSHandler.getPlayerGUISection().get(p).get(slot);

                    if (key.equalsIgnoreCase("back_to_reward_editor_menu")) {
                        XSItemmails xsItemmails = XSHandler.getXsItemmailsHashMap().get(XSHandler.getPlayerEditorKey().get(p));
                        p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),p,xsItemmails));
                    }
                }
            }
        } else if(e.getView().getTitle().equalsIgnoreCase(XSUtils.decodeText(menuConfig.getConfig(XS_MENU_FILE.XS_INVENTORY).getString("settings.title")))) {
            e.setCancelled(true);
            int slot = e.getSlot();

            if(!e.getClickedInventory().equals(e.getView().getBottomInventory()) && XSHandler.getPlayerGUISection().get(p).containsKey(slot)) {
                String key = XSHandler.getPlayerGUISection().get(p).get(slot);

                if (key.equalsIgnoreCase("close")) {
                    p.closeInventory();
                } else if(key.equalsIgnoreCase("next_button_inventory_reward")) {
                    XSHandler.getPlayerPage().put(p,XSHandler.getPlayerPage().get(p)+1);
                    XSUtils.updateInventoryContent(menuConfig.getConfig(XS_MENU_FILE.XS_INVENTORY),p,null);
                } else if(key.equalsIgnoreCase("back_button_inventory_reward")) {
                    if(XSHandler.getPlayerPage().get(p) > 1) {
                        XSHandler.getPlayerPage().put(p,XSHandler.getPlayerPage().get(p)-1);
                        XSUtils.updateInventoryContent(menuConfig.getConfig(XS_MENU_FILE.XS_INVENTORY),p,null);
                    }
                } else {
                    if(e.getClick().equals(ClickType.LEFT)) {
                        int playerIDRef = XSHandler.getPlayerDataReference().get(p.getName());

                        if(!XSHandler.getXsRewardsHashMap().get(playerIDRef).containsKey(key)) {
                            XSUtils.sendMessageFromConfig("reward_null",p);
                            p.closeInventory();
                            return;
                        }

                        if (XSHandler.getCooldownReward().containsKey(p.getUniqueId())) {
                            if(System.currentTimeMillis()-XSHandler.getCooldownReward().get(p.getUniqueId()) <= 0L) {

                                double seconds = (XSHandler.getCooldownReward().get(p.getUniqueId())  - System.currentTimeMillis()) / 1000.0;

                                double secondsRounded = Math.round(seconds * 10.0) / 10.0;

                                Audience senderAudience = (Audience) p;
                                senderAudience.sendMessage(MiniMessage.builder().build().deserialize(messagesConfig.customConfig.getString("prefix") +
                                        messagesConfig.customConfig.getString("wait_a_seconds").replace("%time%",secondsRounded+"")));

                                return;
                            }
                        }

                        XSHandler.getCooldownReward().put(p.getUniqueId(),System.currentTimeMillis()+2000L);

                        String xsRewardsKey = XSHandler.getXsRewardsHashMap().get(playerIDRef).get(key).getIdKeyReward();
                        int count = XSHandler.getXsRewardsHashMap().get(playerIDRef).get(key).getCount();
                        int countStack = 0;

                        XSItemmails xsItemmails = XSHandler.getXsItemmailsHashMap().get(xsRewardsKey);

                        for(String rewardItem : xsItemmails.getRewardItems()) {
                            ItemStack it = XSUtils.itemStackFromBase64(rewardItem);

                            int totalCount = it.getAmount()*count;

                            //Bukkit.broadcastMessage("Count : " + totalCount + " Max Stack " + it.getMaxStackSize());
                            //Bukkit.broadcastMessage("Added : " + ((double)totalCount/(double) it.getMaxStackSize()) + " ; " + Math.ceil((double)totalCount/(double)it.getMaxStackSize()));

                            countStack = (int) (countStack + Math.ceil((double)totalCount/(double)it.getMaxStackSize()));
                        }

                        int emptySlot = 0;
                        for (int i = 0; i < 36; i++) {

                            if(p.getInventory().getContents()[i] == null) {
                                emptySlot++;
                            }

                        }
                        if(emptySlot < countStack) {
                            XSUtils.sendMessageFromConfig("inventory_slot_full", p);
                            return;
                        }
                        XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(),XS_REDIS_MESSAGES.SENT_ITEM_REQUEST_TO_SERVER+"<SPLIT>"+playerIDRef+";"+key+";"+p.getName()+";"+XSHandler.getServerClient());
                    }
                }
            }
        } else {
            if(XSHandler.getPlayerEditOtherKey().containsKey(p.getName())) {
                if(e.getView().getTitle().equalsIgnoreCase(XSUtils.decodeText(menuConfig.getConfig(XS_MENU_FILE.XS_OTHER_INVENTORY).getString("settings.title")).replace("%target%",XSHandler.getPlayerEditOtherKey().get(p.getName())))) {

                    e.setCancelled(true);
                    int slot = e.getSlot();

                    if(!e.getClickedInventory().equals(e.getView().getBottomInventory()) && XSHandler.getPlayerGUISection().get(p).containsKey(slot)) {
                        String key = XSHandler.getPlayerGUISection().get(p).get(slot);

                        if(e.getClick().equals(ClickType.DROP)) {
                            // p.sendMessage(key);

                            String targetName = XSHandler.getPlayerEditOtherKey().get(p.getName());
                            int idRef = XSHandler.getPlayerDataReference().get(targetName);

                            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(),XS_REDIS_MESSAGES.DELETE_REWARD_SPECIFIC_PLAYER_TO_SERVER+"<SPLIT>"+
                                    idRef+";"+key+";"+p.getName()+";"+XSHandler.getServerClient());
                        } else if(e.getClick().equals(ClickType.LEFT)) {
                            if (key.equalsIgnoreCase("close")) {
                                p.closeInventory();
                            } else if(key.equalsIgnoreCase("next_button_inventory_reward")) {
                                XSHandler.getPlayerPage().put(p,XSHandler.getPlayerPage().get(p)+1);
                                XSUtils.updateInventoryContent(menuConfig.getConfig(XS_MENU_FILE.XS_OTHER_INVENTORY),p,null);
                            } else if(key.equalsIgnoreCase("back_button_inventory_reward")) {
                                if(XSHandler.getPlayerPage().get(p) > 1) {
                                    XSHandler.getPlayerPage().put(p,XSHandler.getPlayerPage().get(p)-1);
                                    XSUtils.updateInventoryContent(menuConfig.getConfig(XS_MENU_FILE.XS_OTHER_INVENTORY),p,null);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
