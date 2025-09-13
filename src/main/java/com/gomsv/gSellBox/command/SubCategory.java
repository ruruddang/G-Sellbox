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

public class SubCategory implements SubCommand {

    private final List<String> categories;

    public SubCategory(List<String> categories) {
        this.categories = categories == null ? Collections.emptyList() : categories;
    }

    @Override public String name() { return "카테고리"; }
    @Override public String permission() { return "gsellbox.category"; }
    @Override public String usage() { return "/판매상자 카테고리 <카테고리>"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player p = (Player) sender;

        if (args.length != 1) {
            CommandUtil.msg(p, "§c사용법: " + usage());
            CommandUtil.msg(p, "§7카테고리: §f" + String.join(", ", categories));
            return true;
        }

        String category = args[0];
        if (!categories.contains(category)) {
            CommandUtil.msg(p, "§c잘못된 카테고리야. §7가능: §f" + String.join(", ", categories));
            return true;
        }

        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            CommandUtil.msg(p, "§c손에 아이템을 들어줘.");
            return true;
        }

        String itemId = ItemIdUtil.getItemId(item);
        if (itemId == null) {
            CommandUtil.msg(p, "§c아이디 추출 실패.");
            return true;
        }

        Bukkit.getScheduler().runTaskAsynchronously(GSellBox.getInstance(), () -> {
            PriceCacheManager pcm = GSellBox.getPriceCacheManager();
            if (pcm.getPrice(itemId) <= 0) {
                Bukkit.getScheduler().runTask(GSellBox.getInstance(), () ->
                        CommandUtil.msg(p, "§c이 아이템은 아직 판매 등록되지 않았어."));
                return;
            }

            pcm.updateCategory(itemId, category);
            Bukkit.getScheduler().runTask(GSellBox.getInstance(), () -> {
                SellBoxInfoListener.clearPriceCache();
                CommandUtil.msg(p, "§a카테고리를 §e" + category + "§a(으)로 변경했어.");
            });
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return com.gomsv.gSellBox.util.CommandUtil.prefixFilter(categories, args[0]);
        }
        return List.of();
    }
}
