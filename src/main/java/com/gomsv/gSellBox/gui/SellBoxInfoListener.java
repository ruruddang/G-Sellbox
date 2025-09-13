package com.gomsv.gSellBox.gui;

import com.gomsv.gSellBox.GSellBox;
import com.gomsv.gSellBox.util.Configs;
import dev.lone.itemsadder.api.CustomBlock;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SellBoxInfoListener implements Listener {

    private static final int CATEGORY_SLOT = 45;
    private static final int PREV_SLOT = 48;
    private static final int SEARCH_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int SORT_SLOT = 53;

    private enum SortMode {
        DEFAULT("기본 (이름순)"),
        PRICE_ASC("가격 오름차순"),
        PRICE_DESC("가격 내림차순");

        private final String displayName;

        SortMode(String displayName) { this.displayName = displayName; }

        public String getDisplayName() { return displayName; }

        public SortMode next() {
            return switch (this) {
                case DEFAULT -> PRICE_ASC;
                case PRICE_ASC -> PRICE_DESC;
                case PRICE_DESC -> DEFAULT;
            };
        }
    }

    private final Map<UUID, Integer> pageMap = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> categoryIndexMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> searchKeywordMap = new ConcurrentHashMap<>();
    private final Map<UUID, SortMode> sortModeMap = new ConcurrentHashMap<>();
    private final Set<UUID> awaitingSearch = Collections.synchronizedSet(new HashSet<>());

    private final DataSource dataSource = GSellBox.getInstance().getDataSource();
    private static List<Record> recordsCache = null;

    private static class Record {
        final String itemId, material, category;
        final double price;
        final Integer customModelData;

        Record(String itemId, double price, String material, Integer customModelData, String category) {
            this.itemId = itemId;
            this.price = price;
            this.material = material;
            this.customModelData = customModelData;
            this.category = category;
        }
    }

    private List<String> categoryOrder() {
        return Configs.categoryOrderWithAll();
    }

    public static void clearPriceCache() {
        recordsCache = null;
    }

    @EventHandler
    public void onShiftRightClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || !e.getPlayer().isSneaking()) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        CustomBlock cb = CustomBlock.byAlreadyPlaced(b);
        if (cb == null || !cb.getNamespacedID().equals(Configs.sellBlockId())) return;

        e.setCancelled(true);
        Player p = e.getPlayer();
        pageMap.put(p.getUniqueId(), 0);
        categoryIndexMap.put(p.getUniqueId(), 0);
        searchKeywordMap.remove(p.getUniqueId());
        sortModeMap.put(p.getUniqueId(), SortMode.DEFAULT);
        loadAndOpenGui(p);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().startsWith(Configs.infoTitlePrefix())) return;
        e.setCancelled(true);

        Player p = (Player) e.getWhoClicked();
        int slot = e.getRawSlot();

        if (slot == CATEGORY_SLOT) {
            searchKeywordMap.remove(p.getUniqueId());
            int currentIndex = categoryIndexMap.getOrDefault(p.getUniqueId(), 0);
            int nextIndex = (currentIndex + 1) % categoryOrder().size();
            categoryIndexMap.put(p.getUniqueId(), nextIndex);
            pageMap.put(p.getUniqueId(), 0);
            loadAndOpenGui(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
            return;
        }

        if (slot == SEARCH_SLOT) {
            awaitingSearch.add(p.getUniqueId());
            p.closeInventory();
            p.sendMessage("§e검색할 아이템 이름을 채팅으로 입력해주세요. (취소: '취소' 입력)");
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return;
        }

        if (slot == SORT_SLOT) {
            SortMode currentMode = sortModeMap.getOrDefault(p.getUniqueId(), SortMode.DEFAULT);
            sortModeMap.put(p.getUniqueId(), currentMode.next());
            pageMap.put(p.getUniqueId(), 0);
            loadAndOpenGui(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.4f);
            return;
        }

        int currentPage = pageMap.getOrDefault(p.getUniqueId(), 0);

        if (slot == PREV_SLOT) {
            if (currentPage > 0) {
                pageMap.put(p.getUniqueId(), currentPage - 1);
                loadAndOpenGui(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        } else if (slot == NEXT_SLOT) {
            String keyword = searchKeywordMap.get(p.getUniqueId());
            int categoryIndex = categoryIndexMap.getOrDefault(p.getUniqueId(), 0);
            List<Record> recordsToDisplay = getCurrentlyDisplayedRecords(getRecordsCache(), keyword, categoryIndex);

            if ((currentPage + 1) * 45 < recordsToDisplay.size()) {
                pageMap.put(p.getUniqueId(), currentPage + 1);
                loadAndOpenGui(p);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID uuid = p.getUniqueId();
        if (!awaitingSearch.remove(uuid)) return;

        e.setCancelled(true);
        String keyword = e.getMessage().trim();
        if (keyword.equalsIgnoreCase("취소")) {
            p.sendMessage("§a검색을 취소했습니다.");
            return;
        }

        searchKeywordMap.put(uuid, keyword.toLowerCase());
        categoryIndexMap.put(uuid, 0);
        pageMap.put(uuid, 0);

        List<Record> allRecords = getRecordsCache();
        Bukkit.getScheduler().runTask(GSellBox.getInstance(), () -> openGui(p, allRecords));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        UUID uuid = p.getUniqueId();

        if (awaitingSearch.contains(uuid)) return;

        if (e.getView().getTitle().startsWith(Configs.infoTitlePrefix())) {
            Bukkit.getScheduler().runTaskLater(GSellBox.getInstance(), () -> {
                InventoryView currentView = p.getOpenInventory();
                if (!currentView.getTitle().startsWith(Configs.infoTitlePrefix())) {
                    pageMap.remove(uuid);
                    categoryIndexMap.remove(uuid);
                    searchKeywordMap.remove(uuid);
                    sortModeMap.remove(uuid);
                }
            }, 1L);
        }
    }

    private void loadAndOpenGui(Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(GSellBox.getInstance(), () -> {
            List<Record> allRecords = getRecordsCache();
            Bukkit.getScheduler().runTask(GSellBox.getInstance(), () -> openGui(player, allRecords));
        });
    }

    private void openGui(Player player, List<Record> allRecords) {
        int page = pageMap.getOrDefault(player.getUniqueId(), 0);
        String keyword = searchKeywordMap.get(player.getUniqueId());
        int categoryIndex = categoryIndexMap.getOrDefault(player.getUniqueId(), 0);
        SortMode sortMode = sortModeMap.getOrDefault(player.getUniqueId(), SortMode.DEFAULT);

        List<Record> recordsToDisplay = getCurrentlyDisplayedRecords(allRecords, keyword, categoryIndex);

        switch (sortMode) {
            case PRICE_ASC -> recordsToDisplay.sort(Comparator.comparingDouble((Record r) -> r.price));
            case PRICE_DESC -> recordsToDisplay.sort(Comparator.comparingDouble((Record r) -> r.price).reversed());
            default -> recordsToDisplay.sort(Comparator.comparing((Record r) -> r.itemId));
        }

        String guiTitle = buildGuiTitle(keyword, categoryIndex);

        Inventory inv = Bukkit.createInventory(null, 54, guiTitle);
        int start = page * 45;

        for (int i = 0; i < 45; i++) {
            int idx = start + i;
            if (idx >= recordsToDisplay.size()) break;
            inv.setItem(i, createItemIcon(recordsToDisplay.get(idx)));
        }

        List<String> order = categoryOrder();
        String currentCategoryName = order.get(categoryIndex);
        String nextCategoryName = order.get((categoryIndex + 1) % order.size());
        ItemStack categoryButton = createButton(Material.MAP, "§a카테고리 변경",
                Arrays.asList("§7현재: §e" + currentCategoryName, "§7다음: §e" + nextCategoryName));
        inv.setItem(CATEGORY_SLOT, categoryButton);

        inv.setItem(PREV_SLOT, createButton(Material.ARROW, "§f이전 페이지", null));
        ItemStack searchButton = createButton(Material.CLOCK, "§f아이템 검색", null);
        ItemMeta searchMeta = searchButton.getItemMeta();
        if (searchMeta != null) {
            searchMeta.setCustomModelData(10066);
            searchButton.setItemMeta(searchMeta);
        }
        inv.setItem(SEARCH_SLOT, searchButton);

        if (start + 45 < recordsToDisplay.size()) {
            inv.setItem(NEXT_SLOT, createButton(Material.ARROW, "§a다음 페이지", null));
        } else {
            inv.setItem(NEXT_SLOT, createButton(Material.ARROW, "§c다음 페이지 없음", null));
        }

        ItemStack sortButton = createButton(Material.OAK_SIGN, "§b정렬 방식 변경",
                Arrays.asList("§7현재: §e" + sortMode.getDisplayName(), "§7다음: §e" + sortMode.next().getDisplayName()));
        inv.setItem(SORT_SLOT, sortButton);

        player.openInventory(inv);
    }

    private List<Record> getCurrentlyDisplayedRecords(List<Record> allRecords, String keyword, int categoryIndex) {
        if (keyword != null && !keyword.isEmpty()) {
            return allRecords.stream()
                    .filter(r -> r.itemId.split("#")[0].toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        } else {
            List<String> order = categoryOrder();
            final String categoryName = order.get(categoryIndex);
            return "전체".equals(categoryName)
                    ? allRecords
                    : allRecords.stream().filter(r -> categoryName.equals(r.category)).collect(Collectors.toList());
        }
    }

    private String buildGuiTitle(String keyword, int categoryIndex) {
        String prefix = Configs.infoTitlePrefix();
        if (keyword != null && !keyword.isEmpty()) {
            return prefix + "검색: " + keyword;
        } else {
            List<String> order = categoryOrder();
            return prefix + order.get(categoryIndex);
        }
    }

    private ItemStack createItemIcon(Record rec) {
        Material mat;
        try {
            mat = Material.valueOf(rec.material);
        } catch (IllegalArgumentException ex) {
            mat = Material.PAPER;
        }
        ItemStack item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (rec.customModelData != null && rec.customModelData > 0) {
            meta.setCustomModelData(rec.customModelData);
            String baseName = rec.itemId.split("#")[0].replace("_", " ").toLowerCase();
            String displayName = Arrays.stream(baseName.split(" "))
                    .filter(s -> !s.isEmpty())
                    .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                    .collect(Collectors.joining(" "));
            meta.setDisplayName("§f" + displayName);
        }

        meta.setLore(Collections.singletonList("§f가격: " + String.format("%,.2f", rec.price) + " $"));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createButton(Material mat, String name, List<String> lore) {
        ItemStack btn = new ItemStack(mat, 1);
        ItemMeta m = btn.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            if (lore != null) m.setLore(lore);
            btn.setItemMeta(m);
        }
        return btn;
    }

    private List<Record> getRecordsCache() {
        if (recordsCache == null) {
            recordsCache = loadAllPriceRecordsFromDB();
        }
        return recordsCache;
    }

    private List<Record> loadAllPriceRecordsFromDB() {
        List<Record> list = new ArrayList<>();
        String sql = "SELECT item_id, price, material, custom_model_data, COALESCE(category, '기타') AS category " +
                "FROM sell_prices ORDER BY item_id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = rs.getString("item_id");
                double price = rs.getDouble("price");
                String mat = rs.getString("material");
                int cmd = rs.getInt("custom_model_data");
                String category = rs.getString("category");
                list.add(new Record(id, price, mat, rs.wasNull() ? null : cmd, category));
            }
        } catch (SQLException ex) {
            GSellBox.getInstance().getLogger().log(Level.SEVERE, "가격 정보 DB 로드 실패", ex);
        }
        return list;
    }
}
