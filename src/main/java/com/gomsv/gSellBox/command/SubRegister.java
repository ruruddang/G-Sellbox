package com.gomsv.gSellBox.command;

import com.gomsv.gSellBox.GSellBox;
import com.gomsv.gSellBox.core.PriceCacheManager;
import com.gomsv.gSellBox.gui.SellBoxInfoListener;
import com.gomsv.gSellBox.util.CommandUtil;
import com.gomsv.gSellBox.util.ItemIdUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class SubRegister implements SubCommand {

    private final List<String> categories;

    public SubRegister(List<String> categories) {
        this.categories = categories == null ? Collections.emptyList() : categories;
    }

    @Override public String name() { return "등록"; }
    @Override public String permission() { return "gsellbox.register"; }
    @Override public String usage() { return "/판매상자 등록 <가격> <카테고리>"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player p = (Player) sender;
        if (args.length < 2) {
            CommandUtil.msg(p, "§c사용법: " + usage());
            CommandUtil.msg(p, "§7카테고리: §f" + String.join(", ", categories));
            return true;
        }

        double price;
        try {
            price = Double.parseDouble(args[0]);
            if (price < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            CommandUtil.msg(p, "§c가격은 0 이상의 숫자여야 해.");
            return true;
        }

        String category = args[1];
        if (!categories.contains(category)) {
            CommandUtil.msg(p, "§c잘못된 카테고리야. §7가능: §f" + String.join(", ", categories));
            return true;
        }

        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            CommandUtil.msg(p, "§c손에 아이템을 들어줘.");
            return true;
        }

        // 메인 스레드에서 안전하게 추출
        String itemId = ItemIdUtil.getItemId(item);
        if (itemId == null) {
            CommandUtil.msg(p, "§c아이템 정보를 추출할 수 없어.");
            return true;
        }
        ItemStack snapshot = item.clone();

        // 비동기 DB I/O
        Bukkit.getScheduler().runTaskAsynchronously(GSellBox.getInstance(), () -> {
            PriceCacheManager pcm = GSellBox.getPriceCacheManager();
            pcm.setPrice(itemId, price, snapshot, category);

            Bukkit.getScheduler().runTask(GSellBox.getInstance(), () -> {
                SellBoxInfoListener.clearPriceCache();
                CommandUtil.msg(p, "§a등록 완료: §e" + category + "§a / §e" + String.format("%,.2f", price) + "§a $");
            });
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return com.gomsv.gSellBox.util.CommandUtil.prefixFilter(categories, args[1]);
        }
        return List.of();
    }
}
