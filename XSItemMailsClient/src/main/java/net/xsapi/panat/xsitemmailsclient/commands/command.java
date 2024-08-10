package net.xsapi.panat.xsitemmailsclient.commands;

import net.xsapi.panat.xsitemmailsclient.config.XS_MENU_FILE;
import net.xsapi.panat.xsitemmailsclient.config.menuConfig;
import net.xsapi.panat.xsitemmailsclient.config.messagesConfig;
import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.handler.XS_ITEMS_EDITOR_TOPICS;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsclient.redis.XS_REDIS_MESSAGES;
import net.xsapi.panat.xsitemmailsclient.utils.XSUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class command implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String arg, @NotNull String[] args) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;

            if(command.getName().equalsIgnoreCase("xsitemmails")) {

                if(args.length == 0) {
                    p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_INVENTORY),p,null));
                } else if(args.length == 1) {
                    if(args[0].equalsIgnoreCase("editor")) {

                        if(!p.hasPermission("xsitemmails.editor")) {
                            XSUtils.sendMessageFromConfig("cancel_create_fail",p);
                            return false;
                        }

                        p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_MAIN_MENU),p,null));
                    }
                } else if(args.length == 2) {
                    if(args[0].equalsIgnoreCase("cancel")) {

                        if(!p.hasPermission("xsitemmails.cancel")) {
                            XSUtils.sendMessageFromConfig("cancel_create_fail",p);
                            return false;
                        }

                        String type = args[1];

                        if(type.equalsIgnoreCase("create")) {
                            if(XSHandler.getPlayerCreatorTopics().containsKey(p) && XSHandler.getPlayerCreatorTopics().get(p).equals(XS_ITEMS_EDITOR_TOPICS.INPUT_NAME)) {
                                XSHandler.getPlayerCreatorTopics().remove(p);
                                XSUtils.sendMessageFromConfig("cancel_create_success",p);
                            } else {
                                XSUtils.sendMessageFromConfig("cancel_create_fail",p);
                            }
                        }

                    }
                } else if(args.length == 4) {

                    if(args[0].equalsIgnoreCase("give")) {

                        if(!p.hasPermission("xsitemmails.give")) {
                            XSUtils.sendMessageFromConfig("cancel_create_fail",p);
                            return false;
                        }

                        String playerName = args[1];
                        String idKey = args[2];

                        try {
                            int amount = Integer.parseInt(args[3]);

                            if(amount < 1) {
                                XSUtils.sendMessageFromConfig("only_positive_number",p);
                                return false;
                            }

                            if(!XSHandler.getXsItemmailsHashMap().containsKey(idKey)) {
                                XSUtils.sendMessageFromConfig("item_key_not_found",p);
                                return false;
                            }

                            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(), XS_REDIS_MESSAGES.GIVE_ITEM_SENT_TO_SERVER+"<SPLIT>"
                            +idKey+";"+amount+";"+playerName+";"+XSHandler.getServerClient()+";"+commandSender.getName());

                            String successMsg = messagesConfig.getConfig().getString("sent_reward_success");
                            String prefix = messagesConfig.getConfig().getString("prefix");
                            successMsg = successMsg.replace("%reward%",idKey).replace("%amount%",String.valueOf(amount)).replace("%player%",playerName);

                            p.sendMessage(XSUtils.decodeText(prefix + successMsg));

                        } catch (NumberFormatException nfe) {
                            XSUtils.sendMessageFromConfig("only_number",p);
                            return false;
                        }

                    }

                }

            }

        } else {
            if(command.getName().equalsIgnoreCase("xsitemmails")) {
                if(args.length == 4) {

                    if(args[0].equalsIgnoreCase("give")) {

                        String playerName = args[1];
                        String idKey = args[2];

                        try {
                            int amount = Integer.parseInt(args[3]);

                            if(amount < 1) {
                                commandSender.sendMessage("Only positive number");
                                return false;
                            }

                            if(!XSHandler.getXsItemmailsHashMap().containsKey(idKey)) {
                                commandSender.sendMessage("Item key is invalid");
                                return false;
                            }

                            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(), XS_REDIS_MESSAGES.GIVE_ITEM_SENT_TO_SERVER+"<SPLIT>"
                                    +idKey+";"+amount+";"+playerName+";"+XSHandler.getServerClient()+";"+commandSender.getName());

                            commandSender.sendMessage("[DEBUG] sent " + idKey + " x" + amount + " to " + playerName);

                        } catch (NumberFormatException nfe) {
                            commandSender.sendMessage("Only number");
                            return false;
                        }

                    }

                }
            }
        }
        return false;
    }

}
