package com.gomsv.gSellBox.util;

import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.function.Consumer;

public class ItemIdUtil {

    public static String getItemId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;

        try {
            String displayName = getCleanDisplayName(item);
            String nbtHash = getNbtHash(item);

            if (displayName != null) {
                return displayName + "#" + nbtHash;
            } else {
                return item.getType().toString() + "#" + nbtHash;
            }
        } catch (Exception e) {
            return item.getType().toString();
        }
    }

    private static String getCleanDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName().replaceAll("§.", "");
        }
        return null;
    }

    private static String getNbtHash(ItemStack item) {
        try {
            NBTItem nbt = new NBTItem(item);
            String fullNbt = nbt.toString();

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fullNbt.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash).substring(0, 12);
        } catch (Exception e) {
            return "no-nbt";
        }
    }

}
