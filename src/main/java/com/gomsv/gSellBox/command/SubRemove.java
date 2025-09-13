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

import java.util.List;

public class SubRemove implements SubCommand {

    @Override public String name() { return "삭제"; }
    @Override public String permission() { return "gsellbox.remove"; }
    @Override public String usage() { return "/판매상자 삭제"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player p = (Player) sender;

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
                        CommandUtil.msg(p, "§7이미 등록되지 않은 아이템이야."));
                return;
            }

            pcm.deletePrice(itemId);
            Bukkit.getScheduler().runTask(GSellBox.getInstance(), () -> {
                SellBoxInfoListener.clearPriceCache();
                CommandUtil.msg(p, "§aDB에서 삭제 완료.");
            });
        });

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}
