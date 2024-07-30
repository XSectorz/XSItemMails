package net.xsapi.panat.xsitemmailsclient.commands;

import net.xsapi.panat.xsitemmailsclient.core;

public class commandLoader {

    public commandLoader() {
        core.getPlugin().getCommand("xsitemmails").setExecutor(new command());
    }

}
