package net.xsapi.panat.xsitemmailsclient.redis;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.xsapi.panat.xsitemmailsclient.config.XS_MENU_FILE;
import net.xsapi.panat.xsitemmailsclient.config.mainConfig;
import net.xsapi.panat.xsitemmailsclient.config.menuConfig;
import net.xsapi.panat.xsitemmailsclient.core;
import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.objects.XSItemmails;
import net.xsapi.panat.xsitemmailsclient.objects.XSRewards;
import net.xsapi.panat.xsitemmailsclient.utils.XSUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        core.getPlugin().getLogger().info("XSItemmails Client trying to connect redis....");
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

                            //core.getPlugin().getLogger().info(("Recieved " + message + " From Server"));

                            XS_REDIS_MESSAGES xsRedisMessages = XS_REDIS_MESSAGES.valueOf(message.split("<SPLIT>")[0]);
                            String args = message.split("<SPLIT>")[1];

                            if (xsRedisMessages.equals(XS_REDIS_MESSAGES.RETURN_CREATE)) {
                                String dataJSON = args.split(";")[0];
                                String playerName = args.split(";")[1];
                                String serverClient = args.split(";")[2];
                                String itemName = args.split(";")[3];

                                Gson gson = new Gson();
                                HashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<HashMap<String, XSItemmails>>(){}.getType());

                                XSHandler.setXsItemmailsHashMap(dataList);

                                if(XSHandler.getServerClient().equalsIgnoreCase(serverClient) && Bukkit.getPlayer(playerName)!= null) {
                                    Player sender = Bukkit.getPlayer(playerName);
                                    XSUtils.sendMessageFromConfig("create_success",sender);
                                    Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
                                        @Override
                                        public void run() {
                                            sender.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),sender,XSHandler.getXsItemmailsHashMap().get(itemName)));
                                        }
                                    }, 1L);
                                }

                            } else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.SEND_DATA_FROM_SERVER)) {
                                String dataJSON = args.split(";")[0];
                                Gson gson = new Gson();
                                HashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<HashMap<String, XSItemmails>>(){}.getType());

                                XSHandler.setXsItemmailsHashMap(dataList);

                            } else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.RETURN_ADD_COMMAND)) {
                                String dataJSON = args.split(";")[0];
                                String playerName = args.split(";")[1];
                                String serverClient = args.split(";")[2];
                                Gson gson = new Gson();
                                HashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<HashMap<String, XSItemmails>>(){}.getType());

                                XSHandler.setXsItemmailsHashMap(dataList);

                                if(XSHandler.getServerClient().equalsIgnoreCase(serverClient) && Bukkit.getPlayer(playerName) != null) {
                                    Player p = Bukkit.getPlayer(playerName);
                                    Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
                                        @Override
                                        public void run() {
                                            p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),p,XSHandler.getXsItemmailsHashMap().get(XSHandler.getPlayerEditorKey().get(p))));
                                        }
                                    }, 1L);
                                }
                            } else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.RETURN_REMOVE_COMMAND)) {
                                String dataJSON = args.split(";")[0];
                                String status = args.split(";")[1];
                                String playerName = args.split(";")[2];
                                String serverClient = args.split(";")[3];
                                String idKey = args.split(";")[4];
                                Gson gson = new Gson();
                                HashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<HashMap<String, XSItemmails>>(){}.getType());

                                XSHandler.setXsItemmailsHashMap(dataList);

                                if(XSHandler.getServerClient().equalsIgnoreCase(serverClient) && Bukkit.getPlayer(playerName) != null) {
                                    Player p = Bukkit.getPlayer(playerName);
                                    if(status.equalsIgnoreCase("fail")) {
                                        XSUtils.sendMessageFromConfig("remove_fail",p);
                                    } else {
                                        XSUtils.sendMessageFromConfig("remove_success",p);
                                    }

                                    Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
                                        @Override
                                        public void run() {
                                            p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),p,XSHandler.getXsItemmailsHashMap().get(idKey)));
                                        }
                                    }, 1L);
                                }
                            }  else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.DELETE_ITEM_TO_CLIENT)) {

                                String dataJSON = args.split(";")[0];
                                String playerName = args.split(";")[1];
                                String serverClient = args.split(";")[2];
                                Gson gson = new Gson();
                                HashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<HashMap<String, XSItemmails>>(){}.getType());

                                XSHandler.setXsItemmailsHashMap(dataList);

                                if(XSHandler.getServerClient().equalsIgnoreCase(serverClient) && Bukkit.getPlayer(playerName) != null) {
                                    Player p = Bukkit.getPlayer(playerName);
                                    Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
                                        @Override
                                        public void run() {
                                            p.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_MAIN_MENU),p,null));
                                        }
                                    }, 1L);
                                }
                            } else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.SENT_PLAYER_REWARD_TO_CLIENT)) {

                                String dataJSON = args.split(";")[0];

                                Gson gson = new Gson();
                                HashMap<Integer, ArrayList<XSRewards>> dataList = gson.fromJson(dataJSON, new TypeToken<HashMap<Integer, ArrayList<XSRewards>>>(){}.getType());

                                XSHandler.setXsRewardsHashMap(dataList);

                            }
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
