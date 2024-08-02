package net.xsapi.panat.xsitemmailsclient.commands;

import net.xsapi.panat.xsitemmailsclient.config.XS_MENU_FILE;
import net.xsapi.panat.xsitemmailsclient.config.menuConfig;
import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.handler.XS_ITEMS_EDITOR_TOPICS;
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

                if(args.length == 1) {
                    if(args[0].equalsIgnoreCase("editor")) {
                        p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_MAIN_MENU),p,null));
                    }
                } else if(args.length == 2) {
                    if(args[0].equalsIgnoreCase("cancel")) {

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
                }

            }

        }
        return false;
    }

}
