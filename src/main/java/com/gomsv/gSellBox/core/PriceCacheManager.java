package com.gomsv.gSellBox.core;

import com.gomsv.gSellBox.data.PriceDAO;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PriceCacheManager {

    private final Map<String, Double> priceMap = new ConcurrentHashMap<>();
    private final PriceDAO priceDAO;

    public PriceCacheManager(PriceDAO priceDAO) {
        this.priceDAO = priceDAO;
        reload();
    }

    public void reload() {
        priceMap.clear();
        priceMap.putAll(priceDAO.loadAllPrices());
    }

    public double getPrice(String itemId) {
        return priceMap.getOrDefault(itemId, 0.0);
    }

    public void setPrice(String itemId, double price, ItemStack item, String category) {
        priceMap.put(itemId, price);
        priceDAO.saveOrUpdatePrice(itemId, price, item, category);
    }

    public void updateCategory(String itemId, String category) {
        priceDAO.updateCategory(itemId, category);
    }

    public void deletePrice(String itemId) {
        priceMap.remove(itemId);
        priceDAO.deletePrice(itemId);
    }

    public Map<String, Double> getPriceMap() {
        return priceMap;
    }
}