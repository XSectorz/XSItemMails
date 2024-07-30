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

    private static HashMap<Player,Integer> playerPage = new HashMap<>();
    private static HashMap<Player,HashMap<Integer,String>> playerGUISection = new HashMap<>();

    public static HashMap<Player,HashMap<Integer,String>> getPlayerGUISection() {
        return playerGUISection;
    }

    public static HashMap<Player,Integer> getPlayerPage() {
        return playerPage;
    }
    public static HashMap<Player,Inventory> getPlayerOpenInventory() {
        return playerOpenInventory;
    }

    public static void initSystem() {

        //Setting up configuration
        new configLoader();

        //Connect to redis server
        XSRedisHandler.redisConnection(); //test connection
        XSRedisHandler.subscribeToChannelAsync(XSRedisHandler.getRedisItemMailsClientChannel(mainConfig.getConfig().getString("configuration.server")));

        //Loading command
        new commandLoader();

        //load event
        new eventLoader();

    }

}
