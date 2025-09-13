package com.gomsv.gSellBox.log;

import java.util.Map;
import java.util.UUID;

public class SellLogEntry {
    private final transient UUID uuid; // 저장 제외
    private final String date;
    private final double totalSilver;
    private final Map<String, Integer> items;

    public SellLogEntry(UUID uuid, String date, double totalSilver, Map<String, Integer> items) {
        this.uuid = uuid;
        this.date = date;
        this.totalSilver = totalSilver;
        this.items = items;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getDate() {
        return date;
    }

    public double getTotalSilver() {
        return totalSilver;
    }

    public Map<String, Integer> getItems() {
        return items;
    }
}
