package net.xsapi.panat.xsitemmailsserver.handler;

import com.google.gson.Gson;
import net.xsapi.panat.xsitemmailsserver.config.configLoader;
import net.xsapi.panat.xsitemmailsserver.config.mainConfig;
import net.xsapi.panat.xsitemmailsserver.database.XSDatabaseHandler;
import net.xsapi.panat.xsitemmailsserver.listeners.eventLoader;
import net.xsapi.panat.xsitemmailsserver.objects.XSItemmails;
import net.xsapi.panat.xsitemmailsserver.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsserver.redis.XS_REDIS_MESSAGES;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class XSHandler {

    private static HashMap<String,HashMap<String, XSItemmails>> itemmailsList = new HashMap<>();

    public static HashMap<String, XSItemmails> getItemmailsList(String server) {

        return itemmailsList.get(server);
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

        //Load data from database
        loadDataEachServer();


    }

    public static void loadDataEachServer() {
        for(String group : mainConfig.getConfig().getSection("group-servers").getKeys()) {
            itemmailsList.put(group,new HashMap<>());
            loadDataFromSQL(group);
        }
        sendDataToEachServer();
    }

    public static void sendDataToSpecificServerGroup(String group) {
        Gson gson = new Gson();
        String dataJSON = gson.toJson(XSHandler.getItemmailsList(group));
        for (String server :  mainConfig.getConfig().getStringList("group-servers." + group)) {
            XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(server), XS_REDIS_MESSAGES.SEND_DATA_FROM_SERVER+"<SPLIT>"+dataJSON);
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

            HashMap<String,XSItemmails> itemsList = new HashMap<>();

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String itemName = resultSet.getString("itemName");
                String itemDisplay = resultSet.getString("itemDisplay");
                String rewardItems = resultSet.getString("rewardItems");
                String rewardCommands = resultSet.getString("rewardCommands");

                ArrayList<String> rewardsItemsList = new ArrayList<>(Arrays.asList(rewardItems.split(",")));
                ArrayList<String> rewardsCommandsList = new ArrayList<>(Arrays.asList(rewardCommands.split(",")));

                XSItemmails xsItemmails = new XSItemmails(id,itemName,itemDisplay,rewardsItemsList,rewardsCommandsList);
                itemsList.put(itemName,xsItemmails);
            }

            itemmailsList.put(server,itemsList);

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
