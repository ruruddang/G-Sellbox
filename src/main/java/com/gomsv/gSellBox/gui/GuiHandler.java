package com.gomsv.gSellBox.gui;

import com.gomsv.gSellBox.GSellBox;
import com.gomsv.gSellBox.core.SellManager;
import com.gomsv.gSellBox.util.Configs;
import com.gomsv.gSellBox.util.ItemIdUtil;
import dev.lone.itemsadder.api.CustomBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GuiHandler implements Listener {

    private boolean isOurGui(InventoryView view) {
        return view != null && Configs.sellGuiTitle().equals(view.getTitle());
    }

    private boolean isSellable(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String itemId = ItemIdUtil.getItemId(item);
        if (itemId == null) return false;
        double price = GSellBox.getPriceCacheManager().getPrice(itemId);
        return price > 0;
    }

    private void deny(Player p) {
        p.sendMessage("§c이 아이템은 판매할 수 없습니다.");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.8f);
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        CustomBlock customBlock = CustomBlock.byAlreadyPlaced(block);
        if (customBlock == null) return;

        if (!Configs.sellBlockId().equals(customBlock.getNamespacedID())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Inventory gui = Bukkit.createInventory(player, 54, Configs.sellGuiTitle());
        List<ItemStack> cached = SellManager.getCachedItems(uuid);
        gui.setContents(cached.toArray(new ItemStack[0]));

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_OPEN, 1.0f, 1.0f);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!isOurGui(event.getView())) return;

        final Player player = (Player) event.getWhoClicked();
        final Inventory top = event.getView().getTopInventory();
        final Inventory clicked = event.getClickedInventory();

        // 쉬프트 클릭
        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            if (clicked != null && clicked.equals(event.getView().getBottomInventory())) {
                ItemStack moving = event.getCurrentItem();
                if (!isSellable(moving)) {
                    event.setCancelled(true);
                    deny(player);
                }
            }
            return;
        }

        // 커서
        if (clicked != null && clicked.equals(top)) {
            switch (event.getAction()) {
                case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> {
                    ItemStack cursor = event.getCursor();
                    if (!isSellable(cursor)) {
                        event.setCancelled(true);
                        deny(player);
                    }
                }
                case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                    int hb = event.getHotbarButton();
                    if (hb >= 0) {
                        ItemStack hotbarItem = player.getInventory().getItem(hb);
                        if (!isSellable(hotbarItem)) {
                            event.setCancelled(true);
                            deny(player);
                        }
                    }
                }
                default -> {}
            }
        }

        // 숫자키
        if (event.getClick() == ClickType.NUMBER_KEY) {
            if (clicked != null && clicked.equals(top)) {
                int hb = event.getHotbarButton();
                if (hb >= 0) {
                    ItemStack hotbarItem = player.getInventory().getItem(hb);
                    if (!isSellable(hotbarItem)) {
                        event.setCancelled(true);
                        deny(player);
                    }
                }
            }
        }

        // 더블 클릭
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
        }

        // GUI 내부 클릭 시 판매 불가 아이템 안내
        if (clicked != null && clicked.equals(top)) {
            ItemStack current = event.getCurrentItem();
            if (current != null && current.getType() != Material.AIR && !isSellable(current)) {
                event.setCancelled(true);
                deny(player);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!isOurGui(event.getView())) return;

        Inventory top = event.getView().getTopInventory();
        final int topSize = top.getSize();

        ItemStack cursor = event.getOldCursor();

        if (!isSellable(cursor)) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topSize) {
                    event.setCancelled(true);
                    deny((Player) event.getWhoClicked());
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!isOurGui(event.getView())) return;

        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        List<ItemStack> validItems = new ArrayList<>();
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            validItems.add(item.clone());
        }

        player.playSound(player.getLocation(), Sound.BLOCK_BARREL_CLOSE, 1.0f, 1.0f);

        if (validItems.isEmpty()) {
            List<ItemStack> cached = SellManager.getCachedItems(player.getUniqueId());
            if (cached != null) cached.clear();
        } else {
            SellManager.cacheItems(player.getUniqueId(), validItems);
        }

        SellManager.settlePlayer(player.getUniqueId());
    }
}
