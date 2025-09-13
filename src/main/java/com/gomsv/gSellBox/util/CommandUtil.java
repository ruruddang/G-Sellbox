package com.gomsv.gSellBox.util;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CommandUtil {
    public static void msg(CommandSender s, String m) { s.sendMessage(m); }

    public static List<String> prefixFilter(List<String> list, String token) {
        if (token == null || token.isEmpty()) return list;
        String low = token.toLowerCase(Locale.ROOT);
        return list.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(low))
                .collect(Collectors.toList());
    }
}
