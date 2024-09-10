package net.xsapi.panat.xsitemmailsserver.handler;

import com.google.gson.Gson;
import net.xsapi.panat.xsitemmailsserver.config.configLoader;
import net.xsapi.panat.xsitemmailsserver.config.mainConfig;
import net.xsapi.panat.xsitemmailsserver.core;
import net.xsapi.panat.xsitemmailsserver.database.XSDatabaseHandler;
import net.xsapi.panat.xsitemmailsserver.listeners.eventLoader;
import net.xsapi.panat.xsitemmailsserver.objects.XSItemmails;
import net.xsapi.panat.xsitemmailsserver.objects.XSRewards;
import net.xsapi.panat.xsitemmailsserver.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsserver.redis.XS_REDIS_MESSAGES;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class XSHandler {

    //<server>:<p_id>:<data>
    private static LinkedHashMap<String,Integer> playerDataReference = new LinkedHashMap<>();
    private static LinkedHashMap<String,LinkedHashMap<Integer,LinkedHashMap<String,XSRewards>>> playerRewardData = new LinkedHashMap<>();
    private static LinkedHashMap<String,LinkedHashMap<String, XSItemmails>> itemmailsList = new LinkedHashMap<>();
    private static ArrayList<String> updatedKey = new ArrayList<>();

    public static HashMap<String,Integer> getPlayerDataReference() {
        return playerDataReference;
    }

    public static LinkedHashMap<String, LinkedHashMap<Integer, LinkedHashMap<String,XSRewards>>> getPlayerRewardData() {
        return playerRewardData;
    }

    public static HashMap<String, XSItemmails> getItemmailsList(String server) {

        if(itemmailsList.containsKey(server)) {
            return itemmailsList.get(server);
        }
        return new HashMap<>();
    }

    public static ArrayList<String> getUpdatedKey() {
        return updatedKey;
    }

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

        //load player data reference
        XSDatabaseHandler.loadPlaterDataReference();
        sendPlayerDataReferenceToAllServer();

        //Load data from database
        loadDataEachServer();

        //Sent data to each server
        sendPlayerRewardToAllServer();

    }

    public static void sendPlayerDataReferenceToAllServer() {
        for(String group : mainConfig.getConfig().getSection("group-servers").getKeys()) {
            sendPlayerDataReferenceToSubServer(group);
        }
    }

    public static void sendPlayerDataReferenceToSpecificSubServer(String serverClient) {
        Gson gson = new Gson();
        String dataJSON = gson.toJson(XSHandler.getPlayerDataReference()).replace(";",":");
       // core.getPlugin().getLogger().info(dataJSON);

        XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(serverClient), XS_REDIS_MESSAGES.SENT_PLAYER_DATA_TO_CLIENT+"<SPLIT>"+dataJSON);
    }

    public static void sendPlayerDataReferenceToSubServer(String serverGroup) {

        Gson gson = new Gson();
        String dataJSON = gson.toJson(XSHandler.getPlayerDataReference()).replace(";",":");;

        for(String group : mainConfig.getConfig().getStringList("group-servers."+serverGroup)) {
            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(group), XS_REDIS_MESSAGES.SENT_PLAYER_DATA_TO_CLIENT+"<SPLIT>"+dataJSON);
        }
    }


    public static void sendPlayerRewardToAllServer() {
        for(String group : mainConfig.getConfig().getSection("group-servers").getKeys()) {
            sendPlayerRewardToSubServer(group);
        }
    }

    public static void sendPlayerRewardToSubServer(String serverGroup) {

        Gson gson = new Gson();
        String dataJSON = gson.toJson(XSHandler.getPlayerRewardData().get(serverGroup));
        for(String group : mainConfig.getConfig().getStringList("group-servers."+serverGroup)) {
            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(group), XS_REDIS_MESSAGES.SENT_PLAYER_REWARD_TO_CLIENT+"<SPLIT>"+dataJSON);
        }
    }


    public static int getItemRewardIDByKey(String serverGroup,String idKey) {

        return XSHandler.getItemmailsList(serverGroup).get(idKey).getId();
    }

    public static void loadDataEachServer() {
        for(String group : mainConfig.getConfig().getSection("group-servers").getKeys()) {
            itemmailsList.put(group,new LinkedHashMap<>());
            loadDataFromSQL(group);

            XSHandler.getPlayerRewardData().put(group,new LinkedHashMap<>());
            XSDatabaseHandler.loadPlayerReward(group);
        }
        sendDataToEachServer();
    }

    public static void sendDataToSpecificServerGroup(XS_REDIS_MESSAGES xsRedisMessages,String group,String args) {
        Gson gson = new Gson();
        String dataJSON = gson.toJson(XSHandler.getItemmailsList(group));
        for (String server :  mainConfig.getConfig().getStringList("group-servers." + group)) {
            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(server), xsRedisMessages+"<SPLIT>"+dataJSON+";"+args);
        }
    }

    public static void sendDataToEachServer() {
        Gson gson = new Gson();
        for(String group : mainConfig.getConfig().getSection("group-servers").getKeys()) {
            String dataJSON = gson.toJson(XSHandler.getItemmailsList(group));
            for (String server :  mainConfig.getConfig().getStringList("group-servers." + group)) {
                XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(server), XS_REDIS_MESSAGES.SEND_DATA_FROM_SERVER+"<SPLIT>"+
                        dataJSON);
            }
        }

    }

    public static void savePlayerData() {
        for(String serverGroup : mainConfig.getConfig().getSection("group-servers").getKeys()) {

            XSDatabaseHandler.savePlayerReward(serverGroup);

        }
    }

    public static String getServergroup(String subServer) {
        return mainConfig.getConfig().getSection("group-servers").getKeys().stream()
                .filter(groupList -> mainConfig.getConfig().getStringList("group-servers." + groupList).contains(subServer))
                .findFirst()
                .orElse("");
    }

    public static void loadDataFromSQL(String server) {

        try {
            Connection connection = DriverManager.getConnection(XSDatabaseHandler.getJDBCUrl(),
                    XSDatabaseHandler.getUSER(),XSDatabaseHandler.getPASS());

            String getAllGuild = "SELECT * FROM " + "xsitemmails_bungee_" + server + "_items";

            PreparedStatement preparedStatement = connection.prepareStatement(getAllGuild);
            ResultSet resultSet = preparedStatement.executeQuery();

            LinkedHashMap<String,XSItemmails> itemsList = new LinkedHashMap<>();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String itemName = resultSet.getString("itemName");
                String itemDisplay = resultSet.getString("itemDisplay");
                String rewardItems = resultSet.getString("rewardItems");
                String rewardCommands = resultSet.getString("rewardCommands");

                ArrayList<String> rewardsItemsList;
                if (rewardItems.trim().isEmpty()) {
                    rewardsItemsList = new ArrayList<>();
                } else {
                    rewardsItemsList = new ArrayList<>(Arrays.asList(rewardItems.split(",")));
                }

                ArrayList<String> rewardsCommandsList;
                if (rewardCommands.trim().isEmpty()) {
                    rewardsCommandsList = new ArrayList<>();
                } else {
                    rewardsCommandsList = new ArrayList<>(Arrays.asList(rewardCommands.split(",")));
                }

                XSItemmails xsItemmails = new XSItemmails(id,itemName,itemDisplay,rewardsItemsList,rewardsCommandsList);
                itemsList.put(itemName,xsItemmails);
            }

            itemmailsList.put(server,itemsList);

            core.getPlugin().getLogger().info("SERVER: " + server);
            core.getPlugin().getLogger().info("RewardSize: " + itemmailsList.get(server).size());

            resultSet.close();
            preparedStatement.close();
            connection.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void insertItemToSQL(Connection connection,String server, String name, String default_preview_item) {
        String insertQuery = "INSERT INTO " + "xsitemmails_bungee_"+server+"_items" + " (itemName, itemDisplay, rewardItems, rewardCommands) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatementInsert = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatementInsert.setString(1, name);
            preparedStatementInsert.setString(2, default_preview_item);
            preparedStatementInsert.setString(3, "");
            preparedStatementInsert.setString(4, "");
            preparedStatementInsert.executeUpdate();

            try (ResultSet generatedKeys = preparedStatementInsert.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    XSItemmails xsItemmails = new XSItemmails(id,name,default_preview_item,new ArrayList<>(),new ArrayList<>());
                    XSHandler.getItemmailsList(server).put(name,xsItemmails);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
