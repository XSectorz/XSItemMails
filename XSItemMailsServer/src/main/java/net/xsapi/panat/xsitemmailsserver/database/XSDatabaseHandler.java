package net.xsapi.panat.xsitemmailsserver.database;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.xsapi.panat.xsitemmailsserver.config.mainConfig;
import net.xsapi.panat.xsitemmailsserver.core;
import net.xsapi.panat.xsitemmailsserver.handler.XSHandler;
import net.xsapi.panat.xsitemmailsserver.objects.XSItemmails;
import net.xsapi.panat.xsitemmailsserver.objects.XSRewards;
import net.xsapi.panat.xsitemmailsserver.redis.XSRedisHandler;
import net.xsapi.panat.xsitemmailsserver.redis.XS_REDIS_MESSAGES;

import java.sql.*;
import java.util.*;

public class XSDatabaseHandler {

    private static String JDBC_URL;
    private static String USER;
    private static String PASS;
    private static String DB_NAME;

    private static String GLOBAL_PLAYER_TABLE = "xsitemmails_bungee_players";

    public static String getGlobalPlayerTable() {
        return GLOBAL_PLAYER_TABLE;
    }

    public static String getDbName() {
        return DB_NAME;
    }

    public static String getJDBCUrl() {
        return JDBC_URL;
    }

    public static String getPASS() {
        return PASS;
    }

    public static String getUSER() {
        return USER;
    }

    private final static String SUB_SQL_QUERY = " ("
            + "id INT PRIMARY KEY AUTO_INCREMENT, "
            + "Reference INT, "
            + "itemData TEXT "
            + ")";

    private final static String ITEM_TABLE_SQL_QUERY = " ("
            + "id INT PRIMARY KEY AUTO_INCREMENT, "
            + "itemName TEXT, "
            + "itemDisplay TEXT, "
            + "rewardItems TEXT, "
            + "rewardCommands TEXT "
            + ")";

    //stored player data
    private final static String MAIN_SQL_QUERY = " ("
            + "id INT PRIMARY KEY AUTO_INCREMENT, "
            + "UUID TEXT, "
            + "playerName TEXT"
            + ")";
    public static void createSQLDatabase() {


        sqlConnection(getGlobalPlayerTable(),MAIN_SQL_QUERY);
        core.getPlugin().getLogger().info("******************************");
        core.getPlugin().getLogger().info("XSItemmails trying to create database....");
        for(String servers : mainConfig.getConfig().getSection("group-servers").getKeys()) {
            sqlConnection("xsitemmails_bungee_" + servers,SUB_SQL_QUERY);
            sqlConnection("xsitemmails_bungee_" + servers + "_items",ITEM_TABLE_SQL_QUERY);
        }
        core.getPlugin().getLogger().info("******************************");

    }

    public static void loadPlaterDataReference() {
        try {
            Connection connection = DriverManager.getConnection(XSDatabaseHandler.getJDBCUrl(), XSDatabaseHandler.getUSER(), XSDatabaseHandler.getPASS());
            Statement statement = connection.createStatement();

            String selectQuery = "SELECT * FROM xsitemmails_bungee_players";
            ResultSet resultSet = statement.executeQuery(selectQuery);

            while (resultSet.next()) {
                int reference =  resultSet.getInt("id");
                String playerName =  resultSet.getString("playerName");

                XSHandler.getPlayerDataReference().put(playerName,reference);

            }

            core.getPlugin().getLogger().info("LOAD DATA " + XSHandler.getPlayerDataReference().size());

            statement.close();
            connection.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadPlayerReward(String serverGroup) {
        try {
            Connection connection = DriverManager.getConnection(XSDatabaseHandler.getJDBCUrl(), XSDatabaseHandler.getUSER(), XSDatabaseHandler.getPASS());
            Statement statement = connection.createStatement();

            String selectQuery = "SELECT * FROM xsitemmails_bungee_" + serverGroup;
            ResultSet resultSet = statement.executeQuery(selectQuery);

            LinkedHashMap<Integer,LinkedHashMap<String,XSRewards>> mapData = new LinkedHashMap<>();

            while (resultSet.next()) {
                int reference =  resultSet.getInt("Reference");
                String itemData = resultSet.getString("itemData");

                LinkedHashMap<String,XSRewards> rewardData = new LinkedHashMap<>();

                for(String reward : itemData.split(";")) {

                    String idReward = reward.split(":")[0];
                    int amount = Integer.parseInt(reward.split(":")[1]);

                    if(!XSHandler.getItemmailsList(serverGroup).containsKey(idReward)) {
                        continue;
                    }

                    UUID uuid = UUID.randomUUID();
                    XSRewards xsRewards = new XSRewards(idReward,amount);

                    rewardData.put(uuid.toString(),xsRewards);

                }
                mapData.put(reference,rewardData);
            }

            XSHandler.getPlayerRewardData().put(serverGroup,mapData);
            statement.close();
            connection.close();


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void savePlayerReward(String serverGroup) {

        try {
            Connection connection = DriverManager.getConnection(XSDatabaseHandler.getJDBCUrl(), XSDatabaseHandler.getUSER(), XSDatabaseHandler.getPASS());

            String checkPlayerQuery = "SELECT EXISTS(SELECT * FROM xsitemmails_bungee_" + serverGroup + " WHERE Reference = ?) AS exist";

           // String query = "UPDATE xsitemmails_bungee_" + serverGroup + " SET itemData = ? WHERE Reference = ?";

            for(Map.Entry<Integer,LinkedHashMap<String,XSRewards>> rewardList : XSHandler.getPlayerRewardData().get(serverGroup).entrySet()) {

               // PreparedStatement updateStatement = connection.prepareStatement(updateQuery);

                PreparedStatement preparedStatement = connection.prepareStatement(checkPlayerQuery);
                preparedStatement.setInt(1, rewardList.getKey());
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    boolean exists = resultSet.getBoolean("exist");
                    String query = "UPDATE xsitemmails_bungee_" + serverGroup + " SET itemData = ? WHERE Reference = ?";

                    StringBuilder result = new StringBuilder();

                    int i = 0;
                    for(Map.Entry<String,XSRewards> rewards : rewardList.getValue().entrySet()) {
                        XSRewards reward = rewards.getValue();

                        result.append(reward.getIdKeyReward()).append(":").append(reward.getCount());
                        if(i < rewardList.getValue().size()-1) {
                            result.append(";");
                        }

                        i++;
                    }

                    if (!exists) {
                        query = "INSERT INTO xsitemmails_bungee_" + serverGroup + " (Reference, itemData) VALUES (?, ?)";
                        PreparedStatement insertStatement = connection.prepareStatement(query);
                        insertStatement.setInt(1, rewardList.getKey());
                        insertStatement.setString(2, result.toString());
                        insertStatement.executeUpdate();
                        insertStatement.close();
                    } else {
                        PreparedStatement updateStatement = connection.prepareStatement(query);
                        updateStatement.setString(1, result.toString());
                        updateStatement.setInt(2, rewardList.getKey());
                        updateStatement.executeUpdate();
                        updateStatement.close();
                        connection.close();
                    }

                }

            }

        }   catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public static void givePlayerReward(String playerName,String serverGroup,String idReward,int amount) {
        try {
            Connection connection = DriverManager.getConnection(XSDatabaseHandler.getJDBCUrl(), XSDatabaseHandler.getUSER(), XSDatabaseHandler.getPASS());
            String checkPlayerQuery = "SELECT id FROM " + getGlobalPlayerTable() + " WHERE playerName = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(checkPlayerQuery);
            preparedStatement.setString(1,playerName);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int id = resultSet.getInt("id");
                core.getPlugin().getLogger().info("PLAYER ID " + id);

                UUID uuid = UUID.randomUUID();

                if(XSHandler.getPlayerRewardData().get(serverGroup).containsKey(id)) {
                    XSHandler.getPlayerRewardData().get(serverGroup).get(id).put(uuid.toString(),new XSRewards(idReward,amount));
                } else {
                    LinkedHashMap<String, XSRewards> rewardMap = new LinkedHashMap<>();
                    rewardMap.put(uuid.toString(), new XSRewards(idReward, amount));
                    XSHandler.getPlayerRewardData().get(serverGroup).put(id,rewardMap);
                }

                XSHandler.sendPlayerRewardToSubServer(serverGroup);

            } else {
                core.getPlugin().getLogger().info("PLAYER NULL");
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveDataToSQL() {
        core.getPlugin().getLogger().info("******************************");
        core.getPlugin().getLogger().info("XSItemmails trying to save data....");
        try {
            Connection connection = DriverManager.getConnection(XSDatabaseHandler.getJDBCUrl(),XSDatabaseHandler.getUSER(),XSDatabaseHandler.getPASS());
            for(String group : mainConfig.getConfig().getSection("group-servers").getKeys()) {
                for(Map.Entry<String, XSItemmails> itemmailsEntry : XSHandler.getItemmailsList(group).entrySet()) {
                    if(XSHandler.getUpdatedKey().contains(itemmailsEntry.getKey())) {
                        updateItems(connection,group,itemmailsEntry.getValue());
                        core.getPlugin().getLogger().info("Contain key : " + itemmailsEntry.getKey() + " updated!");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        core.getPlugin().getLogger().info("Saved data complete!");
        core.getPlugin().getLogger().info("******************************");
    }

    private static void updateItems(Connection connection,String group,XSItemmails xsItemmails) {
        String updateQuery = "UPDATE " + ("xsitemmails_bungee_"+group+"_items") + " SET itemDisplay = ?, rewardItems = ?, rewardCommands = ? WHERE id = ?";

        try (PreparedStatement preparedStatementInsert = connection.prepareStatement(updateQuery)) {
            preparedStatementInsert.setString(1, xsItemmails.getItemDisplay());
            preparedStatementInsert.setString(2, String.join(",",xsItemmails.getRewardItems()));
            preparedStatementInsert.setString(3, String.join(",",xsItemmails.getRewardCommands()));
            preparedStatementInsert.setInt(4, xsItemmails.getId());
            preparedStatementInsert.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void removeFromDatabase(String group,int id,String key) {

        String removeItems = "DELETE FROM xsitemmails_bungee_" + group + "_items WHERE id = ?";

        try {
            Connection connection = DriverManager.getConnection(XSDatabaseHandler.getJDBCUrl(),XSDatabaseHandler.getUSER(),XSDatabaseHandler.getPASS());
            PreparedStatement preparedStatement = connection.prepareStatement(removeItems);
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.close();

            XSHandler.getItemmailsList(group).remove(key);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createUserSQL(ProxiedPlayer p) {

        try {
            Connection connection = DriverManager.getConnection(getJDBCUrl(),getUSER(),getPASS());

            String checkPlayerQuery = "SELECT EXISTS(SELECT * FROM " + getGlobalPlayerTable() + " WHERE playerName = ?) AS exist";
            PreparedStatement preparedStatement = connection.prepareStatement(checkPlayerQuery);
            preparedStatement.setString(1, p.getName());
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                boolean exists = resultSet.getBoolean("exist");

                if (!exists) {
                    String insertQuery = "INSERT INTO " + getGlobalPlayerTable() + " (UUID, playerName) "
                            + "VALUES (?, ?)";

                    try (PreparedStatement preparedStatementInsert = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
                        preparedStatementInsert.setString(1, String.valueOf(p.getUniqueId()));
                        preparedStatementInsert.setString(2, p.getName());
                        preparedStatementInsert.executeUpdate();

                        try (ResultSet generatedKeys = preparedStatementInsert.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int id = generatedKeys.getInt(1);

                                XSHandler.getPlayerDataReference().put(p.getName(),id);
                                for(String group : mainConfig.getConfig().getSection("group-servers").getKeys()) {
                                    for(String subServer : mainConfig.getConfig().getStringList("group-servers." + group)) {
                                        XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(subServer), XS_REDIS_MESSAGES.SENT_PLAYER_DATA_TO_CLIENT_SPECIFIC+"<SPLIT>"+p.getName()+";"+id);
                                    }
                                }
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                } else {

                }
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    public static void sqlConnection(String table,String query) {
        String host = mainConfig.getConfig().getString("database.host");
        DB_NAME =  mainConfig.getConfig().getString("database.dbName");
        JDBC_URL = "jdbc:mysql://" + host +  "/" + getDbName();
        USER = mainConfig.getConfig().getString("database.user");
        PASS = mainConfig.getConfig().getString("database.password");


        try {
            Connection connection = DriverManager.getConnection(getJDBCUrl(),getUSER(),getPASS());

            Statement statement = connection.createStatement();

            String createTableQuery = "CREATE TABLE IF NOT EXISTS " + table + query;
            statement.executeUpdate(createTableQuery);
            statement.close();
            connection.close();
            core.getPlugin().getLogger().info("Database : create " + table);
        } catch (SQLException e) {
            core.getPlugin().getLogger().info("Database : could not create " + table);
            e.printStackTrace();
        }
    }

}
