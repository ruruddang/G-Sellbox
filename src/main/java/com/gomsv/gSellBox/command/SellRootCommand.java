package com.gomsv.gSellBox.command;

import com.gomsv.gSellBox.GSellBox;
import com.gomsv.gSellBox.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class SellRootCommand implements TabExecutor {

    private final Map<String, SubCommand> subs = new LinkedHashMap<>();
    private final List<String> categories;

    public SellRootCommand(GSellBox plugin) {
        FileConfiguration cfg = plugin.getConfig();
        this.categories = cfg.getStringList("categories");

        subs.put("등록", new SubRegister(categories));
        subs.put("리로드", new SubReload());
        subs.put("삭제", new SubRemove());
        subs.put("카테고리", new SubCategory(categories)); // ★ 추가
        subs.put("_open", new SubOpen());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length == 0) {
            CommandUtil.msg(sender, "§e사용법: /" + label + " <닉네임|등록|리로드|삭제|카테고리> ...");
            return true;
        }

        String sub = args[0];
        SubCommand sc = subs.get(sub);

        if (sc == null) {
            sc = subs.get("_open");
            return sc.execute(sender, args);
        }

        if (sc.permission() != null && !sender.hasPermission(sc.permission())) {
            CommandUtil.msg(sender, "§c권한이 없습니다.");
            return true;
        }
        if (sc.playerOnly() && !(sender instanceof org.bukkit.entity.Player)) {
            CommandUtil.msg(sender, "§c플레이어만 사용할 수 있습니다.");
            return true;
        }

        return sc.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("등록", "리로드", "삭제", "카테고리"));
            Bukkit.getOnlinePlayers().forEach(p -> base.add(p.getName()));
            return CommandUtil.prefixFilter(base, args[0]);
        }

        SubCommand sc = subs.get(args[0]);
        if (sc == null) return List.of();
        return sc.tabComplete(sender, Arrays.copyOfRange(args, 1, args.length));
    }
}
