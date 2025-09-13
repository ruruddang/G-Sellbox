package com.gomsv.gSellBox.command;

import com.gomsv.gSellBox.GSellBox;
import com.gomsv.gSellBox.core.PriceCacheManager;
import com.gomsv.gSellBox.gui.SellBoxInfoListener;
import com.gomsv.gSellBox.util.CommandUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SubReload implements SubCommand {
    @Override public String name() { return "리로드"; }
    @Override public String permission() { return "gsellbox.reload"; }
    @Override public String usage() { return "/판매상자 리로드"; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        GSellBox.getInstance().reloadConfig();

        PriceCacheManager cache = GSellBox.getPriceCacheManager();
        cache.reload();

        SellBoxInfoListener.clearPriceCache();
        CommandUtil.msg(sender, "§aDB/설정(카테고리·타이틀·반올림) 리로드 완료.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) { return List.of(); }
}
