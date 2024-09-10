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
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.*;

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
                                LinkedHashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<String, XSItemmails>>(){}.getType());

                                XSHandler.setXsItemmailsHashMap(dataList);

                                if(XSHandler.getServerClient().equalsIgnoreCase(serverClient) && Bukkit.getPlayer(playerName)!= null) {
                                    Player sender = Bukkit.getPlayer(playerName);
                                    XSUtils.sendMessageFromConfig("create_success",sender);
                                    Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
                                        @Override
                                        public void run() {
                                            XSHandler.getPlayerEditorKey().put(Bukkit.getPlayer(playerName),itemName);
                                            sender.openInventory(XSUtils.createInventoryFromConfig(menuConfig.getConfig(XS_MENU_FILE.XS_ITEM_CREATE),sender,XSHandler.getXsItemmailsHashMap().get(itemName)));
                                        }
                                    }, 1L);
                                }

                            } else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.SEND_DATA_FROM_SERVER)) {
                                String dataJSON = args.split(";")[0];
                                Gson gson = new Gson();
                                LinkedHashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<String, XSItemmails>>(){}.getType());

                                XSHandler.setXsItemmailsHashMap(dataList);

                            } else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.RETURN_ADD_COMMAND)) {
                                String dataJSON = args.split(";")[0];
                                String playerName = args.split(";")[1];
                                String serverClient = args.split(";")[2];
                                Gson gson = new Gson();
                                LinkedHashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<String, XSItemmails>>(){}.getType());

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
                                LinkedHashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<String, XSItemmails>>(){}.getType());

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
                                LinkedHashMap<String, XSItemmails> dataList = gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<String, XSItemmails>>(){}.getType());

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
                                LinkedHashMap<Integer, LinkedHashMap<String,XSRewards>> dataList = gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<Integer,  LinkedHashMap<String,XSRewards>>>(){}.getType());

                                XSHandler.setXsRewardsHashMap(dataList);
                               // Bukkit.getLogger().info("SET NEW DATA LIST.....");

                            } else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.SENT_PLAYER_DATA_TO_CLIENT_SPECIFIC)) {

                                String playerName = args.split(";")[0];
                                int idRef = Integer.parseInt(args.split(";")[1]);

                                XSHandler.getPlayerDataReference().put(playerName,idRef);
                                //Bukkit.broadcastMessage("PLAYER " + playerName + " join with ref " + idRef);

                            } else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.SENT_PLAYER_DATA_TO_CLIENT)) {
                                String dataJSON = args.split(";")[0];

                                //core.getPlugin().getLogger().info(dataJSON);

                                Gson gson = new Gson();
                                LinkedHashMap<String, Integer> dataList = gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<String, Integer>>(){}.getType());

                               /* for(Map.Entry<String,Integer> data : dataList.entrySet()) {
                                    Bukkit.broadcastMessage("Player " + data.getKey() + " with id " + data.getValue());
                                }*/
                                XSHandler.setPlayerDataReference(dataList);

                            }  else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.SENT_ITEM_SENT_TO_CLIENT)) {

                                String response = args.split(";")[0];
                                String rewardID = args.split(";")[1];
                                int rewardCount = Integer.parseInt(args.split(";")[2]);
                                String playerName = args.split(";")[3];
                                String dataJSON = args.split(";")[4];

                                //Set new data before do something
                                Gson gson = new Gson();
                                LinkedHashMap<Integer, LinkedHashMap<String,XSRewards>> dataList = gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<Integer,  LinkedHashMap<String,XSRewards>>>(){}.getType());
                                XSHandler.setXsRewardsHashMap(dataList);

                                Player p = Bukkit.getPlayer(playerName);

                                if(response.equalsIgnoreCase("reward_check_pass")) {

                                    XSItemmails xsItemmails = XSHandler.getXsItemmailsHashMap().get(rewardID);

                                    //Bukkit.getLogger().info("Reward: " + xsItemmails.getRewardItems());
                                    //Bukkit.getLogger().info("Command: " + xsItemmails.getRewardCommands());
                                    //Bukkit.getLogger().info("Count: " + rewardCount);

                                    for(String rewardItem : xsItemmails.getRewardItems()) {

                                        ItemStack it = XSUtils.itemStackFromBase64(rewardItem);

                                        int totalAmount = it.getAmount()*rewardCount;

                                        int splitToStack = (int) Math.floor((double) totalAmount / (double) it.getMaxStackSize());

                                        //Bukkit.broadcastMessage("Split to stack : " + splitToStack + " and " + (totalAmount-splitToStack*it.getMaxStackSize()));

                                        for(int i = 0 ; i < splitToStack ; i ++) {

                                            it.setAmount(it.getMaxStackSize());
                                            p.getInventory().addItem(it);
                                        }

                                        it.setAmount((totalAmount-splitToStack*it.getMaxStackSize()));
                                        p.getInventory().addItem(it);

                                    }

                                    Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
                                        @Override
                                        public void run() {
                                            for(String data : xsItemmails.getRewardCommands()) {

                                                String type = data.split(" ")[0];

                                                String command = String.join(" ", Arrays.copyOfRange(data.split(" "), 1, data.split(" ").length));
                                                int caseReward = -1;

                                                if(command.contains("%count%")) {
                                                    caseReward = 0;
                                                    command = command.replace("%count%",String.valueOf(rewardCount));
                                                } else {
                                                    caseReward = 1;
                                                }
                                                command = command.replace("%player%",p.getName());

                                                CommandSender commandSender;

                                                if(type.equalsIgnoreCase("[PLAYER]")) {
                                                    commandSender = p;
                                                } else {
                                                    commandSender = Bukkit.getConsoleSender();
                                                }

                                                //Bukkit.broadcastMessage("RUN " + command);
                                                if(caseReward == 1) {
                                                    for(int i = 0 ; i < rewardCount ; i++) {
                                                        Bukkit.dispatchCommand(commandSender,command);
                                                    }
                                                } else {
                                                    Bukkit.dispatchCommand(commandSender,command);
                                                }


                                            }
                                        }
                                    }, 1L);


                                } else {
                                    Bukkit.getLogger().info("Player : " + playerName + " fail to get " + rewardID + " with " + rewardCount);
                                }


                                FileConfiguration fileConfiguration = menuConfig.getConfig(XS_MENU_FILE.XS_INVENTORY);
                                int sizeSlot = fileConfiguration.getStringList("settings.additional_info.rewards_slot").size();
                                int playerIDRef = XSHandler.getPlayerDataReference().get(playerName);
                                int rewardSize = XSHandler.getXsRewardsHashMap().get(playerIDRef).size();

                                int currentPage = (int) Math.ceil((double) rewardSize /(double) sizeSlot);

                                //p.sendMessage("reward " + rewardSize + " / " + sizeSlot);
                                //p.sendMessage("Current" + currentPage);
                                //p.sendMessage("Player Page" +   XSHandler.getPlayerPage().get(p));


                                if(rewardSize == 0) {
                                    XSHandler.getPlayerPage().put(p,1);
                                } else {
                                    if((XSHandler.getPlayerPage().get(p) * sizeSlot )+ 1 > rewardSize) {
                                        XSHandler.getPlayerPage().put(p,currentPage);
                                    }
                                }
                                XSUtils.updateInventoryContent(fileConfiguration,p,null);




                            } else if (xsRedisMessages.equals(XS_REDIS_MESSAGES.DELETE_REWARD_SPECIFIC_PLAYER_TO_CLIENT)) {
                                String dataJSON = args.split(";")[0];
                                String senderName = args.split(";")[1];

                                Gson gson = new Gson();
                                LinkedHashMap<Integer, LinkedHashMap<String,XSRewards>> dataList = gson.fromJson(dataJSON, new TypeToken<LinkedHashMap<Integer,  LinkedHashMap<String,XSRewards>>>(){}.getType());
                                XSHandler.setXsRewardsHashMap(dataList);

                                Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
                                    @Override
                                    public void run() {
                                        FileConfiguration fileConfiguration = menuConfig.getConfig(XS_MENU_FILE.XS_OTHER_INVENTORY);
                                        int sizeSlot = fileConfiguration.getStringList("settings.additional_info.rewards_slot").size();
                                        int playerIDRef = XSHandler.getPlayerDataReference().get(XSHandler.getPlayerEditOtherKey().get(senderName));
                                        int rewardSize = XSHandler.getXsRewardsHashMap().get(playerIDRef).size();

                                        int currentPage = (int) Math.ceil((double) rewardSize /(double) sizeSlot);

                                        Player p = Bukkit.getPlayer(senderName);

                                        if((XSHandler.getPlayerPage().get(p) * sizeSlot )+ 1 > rewardSize) {
                                            XSHandler.getPlayerPage().put(p,currentPage);
                                        }
                                        XSUtils.updateInventoryContent(fileConfiguration,p,null);
                                    }
                                }, 1L);
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
