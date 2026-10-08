package com.hex.aibridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 连接设置（地址 / API key / 模型），由 /ai setup 界面读写。
 * 每次请求前按文件修改时间重载，单人/局域网都能即时生效。
 */
public class Settings {

    public static volatile String baseUrl = "";
    public static volatile String apiKey = "";
    public static volatile String model = "";

    private static long lastMtime = -1L;

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("aibridge-settings.json");
    }

    public static synchronized void reloadIfChanged() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            long m = Files.getLastModifiedTime(f).toMillis();
            if (m == lastMtime) return;
            lastMtime = m;
            JsonObject j = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            baseUrl = j.has("baseUrl") ? j.get("baseUrl").getAsString() : "";
            apiKey = j.has("apiKey") ? j.get("apiKey").getAsString() : "";
            model = j.has("model") ? j.get("model").getAsString() : "";
        } catch (Exception ignored) {
        }
    }

    public static synchronized void save(String url, String key, String mdl) {
        baseUrl = url.trim();
        apiKey = key.trim();
        model = mdl.trim();
        try {
            JsonObject j = new JsonObject();
            j.addProperty("baseUrl", baseUrl);
            j.addProperty("apiKey", apiKey);
            j.addProperty("model", model);
            Files.writeString(file(), j.toString(), StandardCharsets.UTF_8);
            lastMtime = Files.getLastModifiedTime(file()).toMillis();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean configured() {
        return !baseUrl.isBlank();
    }
}
