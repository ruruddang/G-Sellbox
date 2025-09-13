package com.gomsv.gSellBox.command;

import com.gomsv.gSellBox.core.SellManager;
import com.gomsv.gSellBox.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SubOpen implements SubCommand {

    @Override public String name() { return "<닉네임>"; }
    @Override public String permission() { return "gsellbox.admin"; }
    @Override public String usage() { return "/판매상자 <닉네임>"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player admin = (Player) sender;
        if (args.length != 1) {
            CommandUtil.msg(admin, "§c사용법: " + usage());
            return true;
        }
        String nick = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(nick);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            CommandUtil.msg(admin, "§c해당 플레이어는 접속 기록이 없습니다.");
            return true;
        }

        List<ItemStack> items = SellManager.getCachedItems(target.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 54, "§0" + target.getName() + "의 판매 상자");
        if (items != null && !items.isEmpty()) {
            inv.setContents(items.toArray(new ItemStack[0]));
        }
        admin.openInventory(inv);
        CommandUtil.msg(admin, "§a" + target.getName() + "님의 판매 상자를 열었습니다.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}
