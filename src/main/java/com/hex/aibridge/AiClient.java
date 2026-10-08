package com.hex.aibridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AiClient {

    public record Message(String role, String content) {}

    public record Reply(String text, List<String> commands) {}

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static CompletableFuture<Reply> ask(List<Message> messages, int maxTokens) {
        Settings.reloadIfChanged();
        if (!Settings.configured()) {
            return CompletableFuture.completedFuture(
                    new Reply("⚙️ 还没配置 AI 服务。输入 /ai setup 打开设置，填地址并选模型。", List.of()));
        }
        if (Settings.model.isBlank()) {
            return CompletableFuture.completedFuture(
                    new Reply("⚙️ 还没选模型。/ai setup 里点「查询模型」，点一个再保存。", List.of()));
        }
        JsonObject body = new JsonObject();
        body.addProperty("model", Settings.model);
        JsonArray msgs = new JsonArray();
        for (Message m : messages) {
            JsonObject o = new JsonObject();
            o.addProperty("role", m.role());
            o.addProperty("content", m.content());
            msgs.add(o);
        }
        body.add("messages", msgs);
        body.addProperty("max_tokens", maxTokens);
        body.addProperty("temperature", Config.TEMPERATURE.get());
        body.addProperty("stream", false);

        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(base() + "/v1/chat/completions"))
                .timeout(Duration.ofMillis(Config.TIMEOUT_MS.get()))
                .header("Content-Type", "application/json")
                .header("User-Agent", "curl/8.7.1")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        String key = Settings.apiKey;
        if (key != null && !key.isBlank()) {
            rb.header("Authorization", "Bearer " + key);
        }

        return HTTP.sendAsync(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(resp -> parse(resp.statusCode(), resp.body()))
                .exceptionally(t -> new Reply("❌ AI 请求失败：" + rootMessage(t)
                        + "（用 /ai setup 检查地址，或服务器是否开机）", List.of()));
    }

    public static CompletableFuture<List<String>> listModels() {
        Settings.reloadIfChanged();
        return listModels(Settings.baseUrl, Settings.apiKey);
    }

    public static CompletableFuture<List<String>> listModels(String baseUrl, String apiKey) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return CompletableFuture.completedFuture(List.of());
        }
        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(trimTrailingSlash(baseUrl) + "/v1/models"))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "curl/8.7.1")
                .GET();
        if (apiKey != null && !apiKey.isBlank()) {
            rb.header("Authorization", "Bearer " + apiKey);
        }
        return HTTP.sendAsync(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(resp -> {
                    List<String> out = new ArrayList<>();
                    if (resp.statusCode() != 200) return out;
                    try {
                        for (JsonElement e : JsonParser.parseString(resp.body()).getAsJsonObject().getAsJsonArray("data")) {
                            JsonObject o = e.getAsJsonObject();
                            String id = o.get("id").getAsString();
                            String status = "";
                            if (o.has("status") && o.getAsJsonObject("status").has("value")) {
                                status = " (" + o.getAsJsonObject("status").get("value").getAsString() + ")";
                            }
                            out.add(id + status);
                        }
                    } catch (Exception ignored) {
                    }
                    return out;
                })
                .exceptionally(t -> List.of());
    }

    private static String base() {
        return trimTrailingSlash(Settings.baseUrl);
    }

    private static String trimTrailingSlash(String b) {
        if (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }

    private static Reply parse(int status, String body) {
        if (status != 200) {
            return new Reply("❌ AI 服务返回 HTTP " + status + "：" + trim(body, 200), List.of());
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String content = json.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
            return extractCommands(content);
        } catch (Exception e) {
            return new Reply("❌ 解析 AI 响应失败：" + trim(body, 200), List.of());
        }
    }

    static Reply extractCommands(String content) {
        List<String> commands = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        for (String line : content.replace("\r\n", "\n").split("\n")) {
            String t = line.trim();
            if (t.startsWith("!CMD ")) {
                commands.add(t.substring(5).trim());
            } else {
                text.append(line).append('\n');
            }
        }
        return new Reply(text.toString().trim(), commands);
    }

    private static String trim(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ");
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        String m = c.getMessage();
        return m == null ? c.getClass().getSimpleName() : m;
    }
}
