package net.xsapi.panat.xsitemmailsserver.handler;

import net.xsapi.panat.xsitemmailsserver.config.configLoader;
import net.xsapi.panat.xsitemmailsserver.database.XSDatabaseHandler;
import net.xsapi.panat.xsitemmailsserver.listeners.eventLoader;
import net.xsapi.panat.xsitemmailsserver.redis.XSRedisHandler;

public class XSHandler {


    public static void initSystem() {

        //Load configuration
        new configLoader();

        //Creating database
        XSDatabaseHandler.createSQLDatabase();

        //Register Listeners
        new eventLoader();

        //Connecting to Redis Server
        XSRedisHandler.redisConnection(); //test connection
        XSRedisHandler.subscribeToChannelAsync(XSRedisHandler.getRedisItemMailsServerChannel());

        XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel("lobby/01"),"test sent to client");
    }

}
