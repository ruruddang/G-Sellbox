package com.gomsv.gSellBox.data;

import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.util.*;
import java.util.Base64;

import com.google.gson.*;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public class SellCacheStore {

    private static final File cacheFolder = new File("plugins/G-SellBox/cache");

    public static void saveAllToDisk(Map<UUID, List<ItemStack>> cache) {
        if (!cacheFolder.exists()) cacheFolder.mkdirs();

        for (Map.Entry<UUID, List<ItemStack>> entry : cache.entrySet()) {
            UUID uuid = entry.getKey();
            List<ItemStack> items = entry.getValue();

            List<String> encodedItems = new ArrayList<>();
            for (ItemStack item : items) {
                try {
                    encodedItems.add(encodeItem(item));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            JsonObject root = new JsonObject();
            JsonArray itemArray = new JsonArray();
            for (String encoded : encodedItems) {
                itemArray.add(encoded);
            }
            root.add("items", itemArray);

            File file = new File(cacheFolder, uuid + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(root, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Map<UUID, List<ItemStack>> loadAllFromDisk() {
        Map<UUID, List<ItemStack>> result = new HashMap<>();

        if (!cacheFolder.exists()) return result;

        for (File file : Objects.requireNonNull(cacheFolder.listFiles())) {
            if (!file.getName().endsWith(".json")) continue;

            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                JsonObject root = gson.fromJson(reader, JsonObject.class);
                JsonArray items = root.getAsJsonArray("items");

                List<ItemStack> itemList = new ArrayList<>();
                for (JsonElement el : items) {
                    String encoded = el.getAsString();
                    itemList.add(decodeItem(encoded));
                }

                UUID uuid = UUID.fromString(file.getName().replace(".json", ""));
                result.put(uuid, itemList);

                // 불러온 후 삭제
                file.delete();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    private static String encodeItem(ItemStack item) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(out);
        dataOut.writeObject(item);
        dataOut.close();
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static ItemStack decodeItem(String encoded) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        BukkitObjectInputStream dataIn = new BukkitObjectInputStream(in);
        ItemStack item = (ItemStack) dataIn.readObject();
        dataIn.close();
        return item;
    }
}
