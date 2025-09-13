package com.gomsv.gSellBox.util;

import com.gomsv.gSellBox.GSellBox;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Configs {
    private static FileConfiguration cfg() {
        return GSellBox.getInstance().getConfig();
    }

    public static String sellGuiTitle() {
        return cfg().getString("gui.sell_title", "§0판매 상자");
    }

    public static String infoTitlePrefix() {
        return cfg().getString("gui.info_title_prefix", "§0판매 아이템: §8");
    }

    public static String sellBlockId() {
        return cfg().getString("itemsadder.sell_block_id", "gom:sell_box");
    }

    public static List<String> categories() {
        List<String> list = cfg().getStringList("categories");
        return (list == null) ? List.of() : list;
    }

    /** "전체" + categories() */
    public static List<String> categoryOrderWithAll() {
        List<String> out = new ArrayList<>();
        out.add("전체");
        out.addAll(categories());
        return out;
    }

    // -------- Economy / Rounding --------
    public static int moneyScale() {
        return cfg().getInt("economy.decimals", 2); // 소수 자리수
    }

    public static RoundingMode moneyRoundingMode() {
        String s = cfg().getString("economy.rounding", "HALF_UP").toUpperCase();
        try {
            return RoundingMode.valueOf(s);
        } catch (Exception ignored) {
            return RoundingMode.HALF_UP;
        }
    }
}
