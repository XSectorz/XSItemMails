package net.xsapi.panat.xsitemmailsclient.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.xsapi.panat.xsitemmailsclient.config.messagesConfig;
import net.xsapi.panat.xsitemmailsclient.core;
import net.xsapi.panat.xsitemmailsclient.handler.XSHandler;
import net.xsapi.panat.xsitemmailsclient.objects.XSItemmails;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.*;

public class XSUtils {

    public static String decodeText(String str) {
        Component parsedMessage = MiniMessage.builder().build().deserialize(str);
        String legacy = LegacyComponentSerializer.legacyAmpersand().serialize(parsedMessage);
        return legacy.replace('&', '§');
    }

    public static String itemStackToBase64(ItemStack itemStack) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(itemStack);
            String base64 = Base64.getEncoder().encodeToString(outputStream.toByteArray());
            dataOutput.close();
            outputStream.close();

            return base64;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void sendMessageFromConfig(String path,Player p) {
        Audience senderAudience = (Audience) p;
        senderAudience.sendMessage(MiniMessage.builder().build().deserialize(messagesConfig.customConfig.getString("prefix") + messagesConfig.customConfig.getString(path)));
    }

    public static ItemStack itemStackFromBase64(String base64) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack itemStack = (ItemStack) dataInput.readObject();
            dataInput.close();
            inputStream.close();
            return itemStack;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Inventory createInventoryFromConfig(FileConfiguration file,Player p,XSItemmails xsItemmails) {

        String title = XSUtils.decodeText(file.getString("settings.title"));
        int size = file.getInt("settings.size");

        XSHandler.getPlayerGUISection().put(p,new HashMap<>());

        Inventory inv = Bukkit.createInventory(null,size,title);

        XSHandler.getPlayerOpenInventory().put(p,inv);
        XSHandler.getPlayerPage().put(p,1);
        updateInventoryContent(file,p,xsItemmails);

        return inv;
    }

    public static void updateInventoryContent(FileConfiguration fileConfiguration, Player p,XSItemmails xsItemmails) {

        Inventory inv = XSHandler.getPlayerOpenInventory().get(p);
        HashMap<Integer,String> guiSection = new HashMap<>();

        for(String content : fileConfiguration.getConfigurationSection("settings.additional_contents").getKeys(false)) {
            int slot = fileConfiguration.getInt("settings.additional_contents." + content + ".slot");
            guiSection.put(slot,content);
            inv.setItem(slot,decodeItemFromConfig("settings.additional_contents." + content,fileConfiguration,p.getName()));
        }

        if(xsItemmails != null) {
            int slot = fileConfiguration.getInt("settings.additional_info.preview_display_items");
            guiSection.put(slot,"preview_display_items");
            inv.setItem(slot,XSUtils.itemStackFromBase64(xsItemmails.getItemDisplay()));
        }

        if(fileConfiguration.get("settings.additional_info.items_generate") != null) {

            int sizeSlot = fileConfiguration.getStringList("settings.additional_info.items_generate").size();
            HashMap<String, XSItemmails> itemList = XSHandler.getXsItemmailsHashMap();
            List<Map.Entry<String, XSItemmails>> entryList = new ArrayList<>(itemList.entrySet());

            int startIndex = 0;
            int endIndex = 0;

            startIndex = (XSHandler.getPlayerPage().get(p)-1)*sizeSlot;
            endIndex = Math.min(startIndex + sizeSlot-1, itemList.size());

            if(endIndex+1 < itemList.size()) {
                int slot = fileConfiguration.getInt("settings.additional_info.next_button");
                guiSection.put(slot,"next_button");
                inv.setItem(slot,decodeItemFromConfig("settings.additional_info.next_button",fileConfiguration,p.getName()));
            }
            if(XSHandler.getPlayerPage().get(p) > 1) {
                int slot = fileConfiguration.getInt("settings.additional_info.back_button");
                guiSection.put(slot,"back_button");
                inv.setItem(slot,decodeItemFromConfig("settings.additional_info.back_button",fileConfiguration,p.getName()));
            }

            if(endIndex+1 < itemList.size()) {
                entryList = entryList.subList(startIndex, endIndex+1);
            } else {
                entryList = entryList.subList(startIndex, endIndex);
            }

            List<String> stringList = fileConfiguration.getStringList("settings.additional_info.items_generate");
            ArrayList<String> slotList = new ArrayList<>(stringList);

            int index = 0;
            for (Map.Entry<String, XSItemmails> entry : entryList) {
                int slot = Integer.parseInt(slotList.get(index));
                guiSection.put(slot,entry.getKey());
                inv.setItem(slot,XSUtils.itemStackFromBase64(entry.getValue().getItemDisplay()));
                index++;
            }
        }

        XSHandler.getPlayerGUISection().put(p,guiSection);

        p.updateInventory();

    }

    public static ItemStack decodeItemFromConfig(String path, Configuration conf, String player) {

        String display = XSUtils.decodeText(conf.getString(path+".display"));
        Material mat;
        int modelData = 0;

        if(conf.get(path+".customModelData") != null) {
            modelData = (conf.getInt(path+".customModelData"));
        }

        int amount = (conf.getInt(path+".amount"));
        ArrayList<String> lores = new ArrayList<>();

        for(String lore : conf.getStringList(path+".lore")) {
            lores.add(XSUtils.decodeText(lore));
        }

        ItemStack it;

        if(conf.getString(path+".material").startsWith("custom_head")) {
            mat = Material.PLAYER_HEAD;
            String value = conf.getString(path+".material").replace("custom_head-","");

            it = new ItemStack(mat, 1);
            SkullMeta meta = (SkullMeta) it.getItemMeta();
            GameProfile profile = new GameProfile(UUID.randomUUID(), "");
            profile.getProperties().put("textures", new Property("textures", value));
            Field profileField;
            meta.setDisplayName(display);
            meta.setLore(lores);
            meta.setCustomModelData(modelData);
            try {
                assert meta != null;
                profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            }
            it.setItemMeta(meta);
        } else if(conf.getString(path+".material").startsWith("player_head")) {
            mat = Material.PLAYER_HEAD;
            String pName = conf.getString(path+".material").replace("player_head-","");
            it = new ItemStack(mat, 1);
            SkullMeta meta = (SkullMeta) it.getItemMeta();

            if(pName.equalsIgnoreCase("%player%")) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(player));
            } else {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(pName));
            }
            meta.setDisplayName(display);
            meta.setLore(lores);
            meta.setCustomModelData(modelData);
            it.setItemMeta(meta);
        } else {
            mat = Material.valueOf(conf.getString(path+".material"));
            it = new ItemStack(mat,amount);
            if(it.getType() != Material.AIR) {
                ItemMeta meta = it.getItemMeta();

                meta.setDisplayName(display);
                meta.setLore(lores);
                meta.setCustomModelData(modelData);
                it.setItemMeta(meta);
            }
        }

        if(it.hasItemMeta()) {
            ItemMeta itMeta = it.getItemMeta();
            itMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            itMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
            it.setItemMeta(itMeta);
        }


        return it;
    }

}
