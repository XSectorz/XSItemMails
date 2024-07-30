package net.xsapi.panat.xsitemmailsclient.handler;

import net.xsapi.panat.xsitemmailsclient.commands.commandLoader;
import net.xsapi.panat.xsitemmailsclient.config.configLoader;
import net.xsapi.panat.xsitemmailsclient.config.mainConfig;
import net.xsapi.panat.xsitemmailsclient.listener.eventLoader;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class XSHandler {

    private static HashMap<Player, Inventory> playerOpenInventory = new HashMap<>();
    private static HashMap<Player, XS_ITEMS_EDITOR_TOPICS> playerCreatorTopics = new HashMap<>();

    private static HashMap<Player,Integer> playerPage = new HashMap<>();
    private static HashMap<Player,HashMap<Integer,String>> playerGUISection = new HashMap<>();

    private static String serverClient;

    public static HashMap<Player, XS_ITEMS_EDITOR_TOPICS> getPlayerCreatorTopics() {
        return playerCreatorTopics;
    }

    public static HashMap<Player,HashMap<Integer,String>> getPlayerGUISection() {
        return playerGUISection;
    }

    public static HashMap<Player,Integer> getPlayerPage() {
        return playerPage;
    }
    public static HashMap<Player,Inventory> getPlayerOpenInventory() {
        return playerOpenInventory;
    }

    public static String getServerClient() {
        return serverClient;
    }

    public static void setServerClient(String server) {
        serverClient = server;
    }

    public static void initSystem() {

        //Setting up configuration
        new configLoader();

        //setting up server
        setServerClient(mainConfig.getConfig().getString("configuration.server"));

        //Connect to redis server
        XSRedisHandler.redisConnection(); //test connection
        XSRedisHandler.subscribeToChannelAsync(XSRedisHandler.getRedisItemMailsClientChannel(mainConfig.getConfig().getString("configuration.server")));

        //Loading command
        new commandLoader();

        //load event
        new eventLoader();

    }

}
