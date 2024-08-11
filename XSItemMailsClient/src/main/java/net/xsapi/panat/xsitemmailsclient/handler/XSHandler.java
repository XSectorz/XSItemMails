package net.xsapi.panat.xsitemmailsclient.handler;

import net.xsapi.panat.xsitemmailsclient.commands.commandLoader;
import net.xsapi.panat.xsitemmailsclient.config.XS_MENU_FILE;
import net.xsapi.panat.xsitemmailsclient.config.configLoader;
import net.xsapi.panat.xsitemmailsclient.config.mainConfig;
import net.xsapi.panat.xsitemmailsclient.config.menuConfig;
import net.xsapi.panat.xsitemmailsclient.listener.eventLoader;
import net.xsapi.panat.xsitemmailsclient.objects.XSItemmails;
import net.xsapi.panat.xsitemmailsclient.objects.XSRewards;
import net.xsapi.panat.xsitemmailsclient.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsclient.redis.XS_REDIS_MESSAGES;
import net.xsapi.panat.xsitemmailsclient.utils.XSUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class XSHandler {

    private static LinkedHashMap<String, String> playerEditOtherKey = new LinkedHashMap<>();
    private static LinkedHashMap<String, Integer> playerDataReference = new LinkedHashMap<>();
    private static LinkedHashMap<Integer, LinkedHashMap<String,XSRewards>> xsRewardsHashMap = new LinkedHashMap<>();
    private static LinkedHashMap<String, XSItemmails> xsItemmailsHashMap = new LinkedHashMap<>();
    private static LinkedHashMap<Player, Inventory> playerOpenInventory = new LinkedHashMap<>();
    private static LinkedHashMap<Player, String> playerEditorKey = new LinkedHashMap<>();
    private static LinkedHashMap<Player, XS_ITEMS_EDITOR_TOPICS> playerCreatorTopics = new LinkedHashMap<>();

    private static LinkedHashMap<Player,Integer> playerPage = new LinkedHashMap<>();
    private static LinkedHashMap<Player,LinkedHashMap<Integer,String>> playerGUISection = new LinkedHashMap<>();

    private static String serverClient;

    public static LinkedHashMap<Integer,LinkedHashMap<String,XSRewards>> getXsRewardsHashMap() { return  xsRewardsHashMap; }

    public static LinkedHashMap<String,Integer> getPlayerDataReference() { return playerDataReference; }

    public static LinkedHashMap<String,String> getPlayerEditOtherKey() {
        return playerEditOtherKey;
    }

    public static void setXsRewardsHashMap(LinkedHashMap<Integer, LinkedHashMap<String,XSRewards>> map) {
        xsRewardsHashMap = map;
    }

    public static LinkedHashMap<Player, XS_ITEMS_EDITOR_TOPICS> getPlayerCreatorTopics() {
        return playerCreatorTopics;
    }
    public static LinkedHashMap<Player, String> getPlayerEditorKey() {
        return playerEditorKey;
    }

    public static LinkedHashMap<Player,LinkedHashMap<Integer,String>> getPlayerGUISection() {
        return playerGUISection;
    }

    public static LinkedHashMap<Player,Integer> getPlayerPage() {
        return playerPage;
    }
    public static LinkedHashMap<Player,Inventory> getPlayerOpenInventory() {
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

    public static void setXsItemmailsHashMap(LinkedHashMap<String,XSItemmails> dataMap) {
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
        sendRequestPlayerReward();
        sendRequestPlayerReference();

    }

    public static void setPlayerDataReference(LinkedHashMap<String,Integer> data) {
        playerDataReference = data;
    }

    public static void sendRequestPlayerReference() {
        XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(),XS_REDIS_MESSAGES.REQUEST_PLAYER_DATA_TO_SERVER+"<SPLIT>"+XSHandler.getServerClient());
    }

    public static void sendRequestPlayerReward() {
        XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsServerChannel(),XS_REDIS_MESSAGES.REQUEST_PLAYER_REWARD_TO_SERVER+"<SPLIT>"+XSHandler.getServerClient());
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
