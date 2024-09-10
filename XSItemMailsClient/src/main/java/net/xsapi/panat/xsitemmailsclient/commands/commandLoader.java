package net.xsapi.panat.xsitemmailsclient.commands;

import net.xsapi.panat.xsitemmailsclient.core;
import net.xsapi.panat.xsitemmailsclient.utils.XSTabComplete;

public class commandLoader {

    public commandLoader() {
        core.getPlugin().getCommand("xsitemmails").setExecutor(new command());
        core.getPlugin().getCommand("xsitemmails").setTabCompleter(new XSTabComplete());
    }

}
