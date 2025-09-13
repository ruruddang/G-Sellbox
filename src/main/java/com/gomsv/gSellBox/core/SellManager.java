package com.gomsv.gSellBox.core;

import com.gomsv.gSellBox.GSellBox;
import com.gomsv.gSellBox.log.SellLogEntry;
import com.gomsv.gSellBox.log.SellLogger;
import com.gomsv.gSellBox.util.ItemIdUtil;
import com.gomsv.gSellBox.util.Configs;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SellManager {

    private static final Map<UUID, List<ItemStack>> sellCache = new ConcurrentHashMap<>();

    public static void cacheItems(UUID uuid, List<ItemStack> items) {
        sellCache.put(uuid, new ArrayList<>(items));
    }

    public static List<ItemStack> getCachedItems(UUID uuid) {
        return sellCache.getOrDefault(uuid, new ArrayList<>());
    }

    public static void clearCache() {
        sellCache.clear();
    }

    public static Map<UUID, List<ItemStack>> getAllCache() {
        return sellCache;
    }

    public static void setAllCache(Map<UUID, List<ItemStack>> cache) {
        sellCache.clear();
        if (cache != null) {
            sellCache.putAll(cache);
        }
    }

    public static void settlePlayer(UUID uuid) {
        List<ItemStack> items = sellCache.get(uuid);
        if (items == null || items.isEmpty()) return;

        Map<String, Integer> idCountMap = new HashMap<>();
        double totalSilver = 0.0;

        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) continue;

            String itemId = ItemIdUtil.getItemId(item);
            if (itemId == null) continue;

            double price = GSellBox.getPriceCacheManager().getPrice(itemId);
            if (price <= 0) continue;

            int amount = item.getAmount();
            idCountMap.put(itemId, idCountMap.getOrDefault(itemId, 0) + amount);
            totalSilver += price * amount;
        }

        if (totalSilver > 0) {
            // (12) 반올림 정책/소수 자리수 적용
            int scale = Configs.moneyScale();
            var mode = Configs.moneyRoundingMode();

            BigDecimal rounded = new BigDecimal(totalSilver).setScale(scale, mode);
            double pay = rounded.doubleValue();

            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            GSellBox.getEconomy().depositPlayer(offline, pay);

            if (offline.isOnline()) {
                Player player = offline.getPlayer();
                if (player == null) return;

                player.sendMessage("§a[판매 정산] §f총 §e" + String.format("%,." + scale + "f", pay) + " §f$를 받았습니다!");
                player.sendMessage("§7판매 내역:");

                for (Map.Entry<String, Integer> entry : idCountMap.entrySet()) {
                    String itemId = entry.getKey();
                    int amount = entry.getValue();
                    double price = GSellBox.getPriceCacheManager().getPrice(itemId);
                    double subtotal = price * amount;
                    String name = itemId.split("#")[0].replace("_", " ");

                    // 표시만 소수 자리 맞춰줌 (개별 항목은 반올림 미적용, 합계만 정책 적용)
                    player.sendMessage("§8- §f" + name + " §7x" + amount + " §8> §e" +
                            String.format("%,." + scale + "f", subtotal) + " §f$");
                }
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            }

            String date = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            SellLogEntry logEntry = new SellLogEntry(uuid, date, pay, idCountMap);
            SellLogger.saveAsync(logEntry);
        }

        sellCache.remove(uuid);
    }
}
