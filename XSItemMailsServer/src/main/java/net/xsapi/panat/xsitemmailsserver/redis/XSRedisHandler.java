package net.xsapi.panat.xsitemmailsserver.redis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.xsapi.panat.xsitemmailsserver.config.mainConfig;
import net.xsapi.panat.xsitemmailsserver.core;
import net.xsapi.panat.xsitemmailsserver.database.XSDatabaseHandler;
import net.xsapi.panat.xsitemmailsserver.handler.XSHandler;
import net.xsapi.panat.xsitemmailsserver.objects.XSItemmails;
import net.xsapi.panat.xsitemmailsserver.objects.XSRewards;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class XSRedisHandler {

    private static String redisHost;
    private static int redisPort;
    private static String redisPass;

    private static String redisItemMailsServerChannel = "XSITEM_MAILS_SERVER";
    private static String redisItemMailsClientChannel = "XSITEM_MAILS_CLIENT";
    public static ArrayList<Thread> threads = new ArrayList<>();

    public static String getRedisItemMailsServerChannel() {
        return redisItemMailsServerChannel;
    }
    public static String getRedisItemMailsClientChannel(String server) {
        return redisItemMailsClientChannel+"_"+server;
    }

    public static String getRedisHost() {
        return redisHost;
    }
    public static String getRedisPass() {
        return redisPass;
    }

    public static int getRedisPort() {
        return redisPort;
    }

    public static void redisConnection() {
        redisHost = mainConfig.getConfig().getString("redis.host");
        redisPort = mainConfig.getConfig().getInt("redis.port");
        redisPass = mainConfig.getConfig().getString("redis.password");

        core.getPlugin().getLogger().info("******************************");
        core.getPlugin().getLogger().info("XSItemmails trying to connect redis....");
        try {
            Jedis jedis = new Jedis(getRedisHost(), getRedisPort());
            if(!getRedisPass().isEmpty()) {
                jedis.auth(getRedisPass());
            }
            jedis.close();
            core.getPlugin().getLogger().info("Redis Server : Connected");
        } catch (Exception e) {
            core.getPlugin().getLogger().info("Redis Server : Not Connected");
            e.printStackTrace();
        }
        core.getPlugin().getLogger().info("******************************");
    }

    public static void subscribeToChannelAsync(String channelName) {
        Thread thread = new Thread(() -> {
            try (Jedis jedis = new Jedis(getRedisHost(), getRedisPort(),0)) {
                if(!getRedisPass().isEmpty()) {
                    jedis.auth(getRedisPass());
                }
                JedisPubSub jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }

                        if(channel.startsWith(channelName)) {

                            XS_REDIS_MESSAGES xsRedisMessages = XS_REDIS_MESSAGES.valueOf(message.split("<SPLIT>")[0]);
                            String args = message.split("<SPLIT>")[1];
                            if(xsRedisMessages.equals(XS_REDIS_MESSAGES.CREATE_ITEM)) {

                                String itemName = args.split(";")[0];
                                String serverClient = args.split(";")[1];
                                String senderName = args.split(";")[2];
                                String base64Items = args.split(";")[3];

                                String serverGroup = XSHandler.getServergroup(serverClient);

                                try {
                                    Connection connection = DriverManager.getConnection(XSDatabaseHandler.getJDBCUrl(),XSDatabaseHandler.getUSER(),XSDatabaseHandler.getPASS());
                                    XSHandler.insertItemToSQL(connection,serverGroup,itemName,base64Items);
                                    //core.getPlugin().getLogger().info("send to " + XSRedisHandler.getRedisItemMailsClientChannel(serverClient));

                                    XSHandler.sendDataToSpecificServerGroup(XS_REDIS_MESSAGES.RETURN_CREATE,serverGroup,senderName+";"+serverClient+";"+itemName);
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }

                            } else if(xsRedisMessages.equals(XS_REDIS_MESSAGES.REQ_DATA_FROM_CLIENT)) {
                                String serverClient = args.split(";")[0];

                                String serverGroup = XSHandler.getServergroup(serverClient);

                                Gson gson = new Gson();
                                String dataJSON = gson.toJson(XSHandler.getItemmailsList(serverGroup));

                                XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(serverClient),XS_REDIS_MESSAGES.SEND_DATA_FROM_SERVER+"<SPLIT>"
                                + dataJSON);
                            } else if(xsRedisMessages.equals(XS_REDIS_MESSAGES.UPDATE_DATA_TO_SERVER)) {
                                String updateCase = args.split(";")[0];
                                String serverClient = args.split(";")[1];
                                String idKey = args.split(";")[2];
                                String serverGroup = XSHandler.getServergroup(serverClient);

                                XSItemmails xsItemmails = XSHandler.getItemmailsList(serverGroup).get(idKey);

                                if(updateCase.equalsIgnoreCase("preview")) {
                                    String itemKey = args.split(";")[3];
                                    xsItemmails.setItemDisplay(itemKey);
                                    XSHandler.sendDataToSpecificServerGroup(XS_REDIS_MESSAGES.SEND_DATA_FROM_SERVER,serverGroup,"");
                                } else if(updateCase.equalsIgnoreCase("item_rewards")) {
                                    String dataJSON = args.split(";")[3];
                                    Gson gson = new Gson();
                                    ArrayList<String> dataList = gson.fromJson(dataJSON, new TypeToken<ArrayList<String>>(){}.getType());
                                    xsItemmails.setRewardItems(dataList);
                                    XSHandler.sendDataToSpecificServerGroup(XS_REDIS_MESSAGES.SEND_DATA_FROM_SERVER,serverGroup,"");
                                } else if(updateCase.equalsIgnoreCase("add_commands")) {
                                    String data = args.split(";")[3];
                                    String playerName = args.split(";")[4];

                                    xsItemmails.getRewardCommands().add(data);
                                    XSHandler.sendDataToSpecificServerGroup(XS_REDIS_MESSAGES.RETURN_ADD_COMMAND,serverGroup,playerName+";"+serverClient);
                                } else if(updateCase.equalsIgnoreCase("remove_commands")) {
                                    String playerName = args.split(";")[3];
                                    String status;
                                    if(xsItemmails.getRewardCommands().isEmpty()) {
                                        status = "fail";
                                    } else {
                                        status = "success";
                                        xsItemmails.getRewardCommands().remove(xsItemmails.getRewardCommands().size()-1);
                                    }

                                    XSHandler.sendDataToSpecificServerGroup(XS_REDIS_MESSAGES.RETURN_REMOVE_COMMAND,serverGroup,status+";"+playerName+";"+serverClient+";"+idKey);

                                }
                                if(!XSHandler.getUpdatedKey().contains(idKey)) {
                                    XSHandler.getUpdatedKey().add(idKey);
                                }

                            }  else if(xsRedisMessages.equals(XS_REDIS_MESSAGES.DELETE_ITEM_TO_SERVER)) {
                                int id = Integer.parseInt(args.split(";")[0]);
                                String key = args.split(";")[1];
                                String serverClient = args.split(";")[2];
                                String playerName = args.split(";")[3];
                                String serverGroup = XSHandler.getServergroup(serverClient);
                                XSDatabaseHandler.removeFromDatabase(serverGroup,id,key);

                                XSHandler.sendDataToSpecificServerGroup(XS_REDIS_MESSAGES.DELETE_ITEM_TO_CLIENT,serverGroup,playerName+";"+serverClient);

                            } else if(xsRedisMessages.equals(XS_REDIS_MESSAGES.GIVE_ITEM_SENT_TO_SERVER)) {

                                String key = args.split(";")[0];
                                int amount = Integer.parseInt(args.split(";")[1]);
                                String playerName = args.split(";")[2];
                                String serverClient = args.split(";")[3];
                                String commandender = args.split(";")[4];

                                String serverGroup = XSHandler.getServergroup(serverClient);
                                XSDatabaseHandler.givePlayerReward(playerName,serverGroup,key,amount);

                            } else if(xsRedisMessages.equals(XS_REDIS_MESSAGES.REQUEST_PLAYER_REWARD_TO_SERVER)) {

                                String serverClient = args.split(";")[0];
                                String serverGroup = XSHandler.getServergroup(serverClient);

                                Gson gson = new Gson();
                                String dataJSON = gson.toJson(XSHandler.getPlayerRewardData().get(serverGroup));
                                XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(serverClient), XS_REDIS_MESSAGES.SENT_PLAYER_REWARD_TO_CLIENT+"<SPLIT>"+dataJSON);
                            } else if(xsRedisMessages.equals(XS_REDIS_MESSAGES.REQUEST_PLAYER_DATA_TO_SERVER)) {

                                String serverClient = args.split(";")[0];

                                XSHandler.sendPlayerDataReferenceToSpecificSubServer(serverClient);
                            } else if(xsRedisMessages.equals(XS_REDIS_MESSAGES.SENT_ITEM_REQUEST_TO_SERVER)) {

                                int idRef = Integer.parseInt(args.split(";")[0]);
                                String uniqueKey = args.split(";")[1];
                                String playerName = args.split(";")[2];
                                String serverClient = args.split(";")[3];
                                String serverGroup = XSHandler.getServergroup(serverClient);

                                XSRewards xsRewards = XSHandler.getPlayerRewardData().get(serverGroup).get(idRef).get(uniqueKey);

                                XSHandler.getPlayerRewardData().get(serverGroup).get(idRef).remove(uniqueKey);
                                Gson gson = new Gson();
                                String dataJSON = gson.toJson(XSHandler.getPlayerRewardData().get(serverGroup));


                                //core.getPlugin().getLogger().info("DEBUG...");
                                for(String server : mainConfig.getConfig().getStringList("group-servers."+serverGroup)) {
                                    //core.getPlugin().getLogger().info("SERVER " + server);
                                    if(!server.equalsIgnoreCase(serverClient)) {
                                        //core.getPlugin().getLogger().info("SENT TO " + server);
                                        XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(server), XS_REDIS_MESSAGES.SENT_PLAYER_REWARD_TO_CLIENT+"<SPLIT>"+dataJSON);
                                    }
                                }

                                if(XSHandler.getItemmailsList(serverGroup).containsKey(xsRewards.getIdKeyReward())) {
                                    XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(serverClient),XS_REDIS_MESSAGES.SENT_ITEM_SENT_TO_CLIENT+"<SPLIT>"
                                    +"reward_check_pass;"+xsRewards.getIdKeyReward()+";"+xsRewards.getCount()+";"+playerName+";"+dataJSON);
                                } else {
                                    XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(serverClient),XS_REDIS_MESSAGES.SENT_ITEM_SENT_TO_CLIENT+"<SPLIT>"
                                            +"reward_check_fail;"+xsRewards.getIdKeyReward()+";"+xsRewards.getCount()+";"+playerName+";"+dataJSON);
                                }

                            }

                           core.getPlugin().getLogger().info(("Recieved " + message + " From Client"));
                        }

                    }
                };
                jedis.subscribe(jedisPubSub, channelName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        threads.add(thread);
    }

    public static void sendRedisMessage(String CHName, String message) {

        new Thread(() -> {
            try (Jedis jedis = new Jedis(getRedisHost(), getRedisPort())) {
                if(!getRedisPass().isEmpty()) {
                    jedis.auth(getRedisPass());
                }
                jedis.publish(CHName, message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void destroyThreads() {
        for(Thread thread : threads) {
            thread.interrupt();
        }
    }

}
