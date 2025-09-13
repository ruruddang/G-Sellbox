package com.gomsv.gSellBox.data;

import org.bukkit.inventory.ItemStack;
import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class PriceDAO {

    private final DataSource dataSource;

    public PriceDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createTableIfNotExists() {
        String sql = """
        CREATE TABLE IF NOT EXISTS sell_prices (
            item_id VARCHAR(64) NOT NULL PRIMARY KEY,
            price DOUBLE NOT NULL,
            material VARCHAR(64) NOT NULL,
            custom_model_data INT,
            category VARCHAR(32) DEFAULT '기타'
        );
        """;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveOrUpdatePrice(String itemId, double price, ItemStack item, String category) {
        String sql = "REPLACE INTO sell_prices(item_id, price, material, custom_model_data, category) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.setDouble(2, price);
            stmt.setString(3, item.getType().name());

            if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
                stmt.setInt(4, item.getItemMeta().getCustomModelData());
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            stmt.setString(5, (category == null || category.isBlank()) ? "기타" : category);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateCategory(String itemId, String category) {
        String sql = "UPDATE sell_prices SET category = ? WHERE item_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            stmt.setString(2, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Double> loadAllPrices() {
        Map<String, Double> map = new HashMap<>();
        String sql = "SELECT item_id, price FROM sell_prices";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("item_id"), rs.getDouble("price"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public void deletePrice(String itemId) {
        String sql = "DELETE FROM sell_prices WHERE item_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}