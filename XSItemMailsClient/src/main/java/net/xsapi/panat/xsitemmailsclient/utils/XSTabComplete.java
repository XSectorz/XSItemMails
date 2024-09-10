package net.xsapi.panat.xsitemmailsclient.utils;

import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class XSTabComplete implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        Player p = (Player) sender;


        if (command.getName().equalsIgnoreCase("xsitemmails") || command.getName().equalsIgnoreCase("itemmails")) {
            if(p.hasPermission("xsitemmails.admin")) {
                if(args.length == 1) {
                    completions.add("editor");
                    completions.add("give");
                } else if (args.length == 2) {
                    if(args[0].equalsIgnoreCase("give")) {
                        for(String user : XSHandler.getPlayerDataReference().keySet()) {
                            if(user.startsWith(args[1])) {
                                completions.add(user);
                            }
                        }
                    }
                } else if (args.length == 3) {
                    if(args[0].equalsIgnoreCase("give")) {
                        return new ArrayList<>(XSHandler.getXsItemmailsHashMap().keySet());
                    }
                } else if (args.length == 4) {
                    if(args[0].equalsIgnoreCase("give")) {
                        completions.add("<amount>");
                    }
                }
            }
        }

        return completions;
    }
}
