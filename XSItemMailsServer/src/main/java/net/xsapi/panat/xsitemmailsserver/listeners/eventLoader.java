package net.xsapi.panat.xsitemmailsserver.listeners;

import net.xsapi.panat.xsitemmailsserver.core;

public class eventLoader {

    public eventLoader() {

        core.getPlugin().getProxy().getPluginManager().registerListener(core.getPlugin(),new onPlayerJoin());

    }

}
