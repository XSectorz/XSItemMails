package net.xsapi.panat.xsitemmailsserver.redis;

import net.xsapi.panat.xsitemmailsserver.config.mainConfig;
import net.xsapi.panat.xsitemmailsserver.core;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;

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
        return redisItemMailsServerChannel+"_"+server;
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

                            if(xsRedisMessages.equals(XS_REDIS_MESSAGES.CREATE_ITEM)) {

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
