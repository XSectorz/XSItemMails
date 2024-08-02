package net.xsapi.panat.xsitemmailsclient.handler;

import net.xsapi.panat.xsitemmailsclient.commands.commandLoader;
import net.xsapi.panat.xsitemmailsclient.config.XS_MENU_FILE;
import net.xsapi.panat.xsitemmailsclient.config.configLoader;
import net.xsapi.panat.xsitemmailsclient.config.mainConfig;
import net.xsapi.panat.xsitemmailsclient.config.menuConfig;
import net.xsapi.panat.xsitemmailsclient.listener.eventLoader;
import net.xsapi.panat.xsitemmailsclient.objects.XSItemmails;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsclient.redis.XS_REDIS_MESSAGES;
import net.xsapi.panat.xsitemmailsclient.utils.XSUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class XSHandler {

    private static HashMap<String, XSItemmails> xsItemmailsHashMap = new HashMap<>();
    private static HashMap<Player, Inventory> playerOpenInventory = new HashMap<>();
    private static HashMap<Player, String> playerEditorKey = new HashMap<>();
    private static HashMap<Player, XS_ITEMS_EDITOR_TOPICS> playerCreatorTopics = new HashMap<>();

    private static HashMap<Player,Integer> playerPage = new HashMap<>();
    private static HashMap<Player,HashMap<Integer,String>> playerGUISection = new HashMap<>();

    private static String serverClient;

    public static HashMap<Player, XS_ITEMS_EDITOR_TOPICS> getPlayerCreatorTopics() {
        return playerCreatorTopics;
    }
    public static HashMap<Player, String> getPlayerEditorKey() {
        return playerEditorKey;
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

    public static HashMap<String, XSItemmails> getXsItemmailsHashMap() {
        return xsItemmailsHashMap;
    }

    public static void setXsItemmailsHashMap(HashMap<String,XSItemmails> dataMap) {
        xsItemmailsHashMap = dataMap;
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

        //Req data
        reqDataFromServer();

    }

    public static void closeAllOpenInventory() {
        for(Player p : Bukkit.getOnlinePlayers()) {
            for(XS_MENU_FILE xsMenuFile : XS_MENU_FILE.values()) {
                if(p.getOpenInventory().getTitle().equalsIgnoreCase(XSUtils.decodeText(menuConfig.getConfig(xsMenuFile).getString("settings.title")))) {
                    p.closeInventory();
                }
            }
        }
    }

    public static void reqDataFromServer() {
        XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(), XS_REDIS_MESSAGES.REQ_DATA_FROM_CLIENT+"<SPLIT>"
        +XSHandler.getServerClient());
    }

}
