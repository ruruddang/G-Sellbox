package com.gomsv.gSellBox.log;

import com.gomsv.gSellBox.GSellBox;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SellLogger {

    private static final File logDir = new File("plugins/G-SellBox/logs");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Type listType = new TypeToken<List<SellLogEntry>>() {}.getType();

    public static void saveAsync(SellLogEntry entry) {
        if (entry == null || entry.getDate() == null || entry.getUuid() == null) return;

        GSellBox.getInstance().getServer().getScheduler().runTaskAsynchronously(GSellBox.getInstance(), () -> {
            try {
                // 날짜 폴더 생성
                String date = entry.getDate().substring(0, 10); // yyyy-MM-dd
                File folder = new File(logDir, date);
                if (!folder.exists()) folder.mkdirs();

                // 파일명: <uuid>.json
                File file = new File(folder, entry.getUuid().toString() + ".json");

                // 기존 로그 읽기
                List<SellLogEntry> entries = new ArrayList<>();
                if (file.exists()) {
                    try (Reader reader = new FileReader(file)) {
                        entries = gson.fromJson(reader, listType);
                        if (entries == null) entries = new ArrayList<>();
                    } catch (IOException e) {
                        GSellBox.getInstance().getLogger().warning("판매 로그 읽기 실패: " + e.getMessage());
                    }
                }

                // 새 로그 추가
                entries.add(entry);

                // 로그 저장
                try (Writer writer = new FileWriter(file)) {
                    gson.toJson(entries, writer);
                }

            } catch (IOException e) {
                GSellBox.getInstance().getLogger().warning("판매 로그 저장 실패: " + e.getMessage());
            } catch (Exception ex) {
                GSellBox.getInstance().getLogger().warning("판매 로그 처리 중 예기치 못한 오류: " + ex.getMessage());
            }
        });
    }
}
