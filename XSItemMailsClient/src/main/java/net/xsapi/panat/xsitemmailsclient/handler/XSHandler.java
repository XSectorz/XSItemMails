package net.xsapi.panat.xsitemmailsclient.handler;

import net.xsapi.panat.xsitemmailsclient.config.configLoader;
import net.xsapi.panat.xsitemmailsclient.config.mainConfig;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;

public class XSHandler {

    public static void initSystem() {

        //Setting up configuration
        new configLoader();

        //Connect to redis server
        XSRedisHandler.redisConnection(); //test connection
        XSRedisHandler.subscribeToChannelAsync(XSRedisHandler.getRedisItemMailsClientChannel(mainConfig.getConfig().getString("configuration.server")));

    }

}
