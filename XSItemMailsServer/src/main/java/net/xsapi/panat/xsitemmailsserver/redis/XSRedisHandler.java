package net.xsapi.panat.xsitemmailsserver.redis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.xsapi.panat.xsitemmailsserver.config.mainConfig;
import net.xsapi.panat.xsitemmailsserver.core;
import net.xsapi.panat.xsitemmailsserver.database.XSDatabaseHandler;
import net.xsapi.panat.xsitemmailsserver.handler.XSHandler;
import net.xsapi.panat.xsitemmailsserver.objects.XSItemmails;
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
                                    XSRedisHandler.sendRedisMessage(XSRedisHandler.getRedisItemMailsClientChannel(serverClient),XS_REDIS_MESSAGES.CREATE_ITEM_RESPOND+"<SPLIT>"+senderName);
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
                                } else if(updateCase.equalsIgnoreCase("item_rewards")) {
                                    String dataJSON = args.split(";")[3];
                                    Gson gson = new Gson();
                                    ArrayList<String> dataList = gson.fromJson(dataJSON, new TypeToken<ArrayList<String>>(){}.getType());
                                    xsItemmails.setRewardItems(dataList);
                                }
                                if(!XSHandler.getUpdatedKey().contains(idKey)) {
                                    XSHandler.getUpdatedKey().add(idKey);
                                }

                                /*Update to all server*/
                                XSHandler.sendDataToSpecificServerGroup(serverGroup);

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
