package net.xsapi.panat.xsitemmailsserver.database;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.xsapi.panat.xsitemmailsserver.config.mainConfig;
import net.xsapi.panat.xsitemmailsserver.core;

import java.sql.*;

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

                    try (PreparedStatement preparedStatementInsert = connection.prepareStatement(insertQuery)) {
                        preparedStatementInsert.setString(1, String.valueOf(p.getUniqueId()));
                        preparedStatementInsert.setString(2, p.getName());
                        preparedStatementInsert.executeUpdate();
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
