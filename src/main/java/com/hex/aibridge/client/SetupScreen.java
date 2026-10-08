package com.hex.aibridge.client;

import com.hex.aibridge.AiClient;
import com.hex.aibridge.Settings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SetupScreen extends Screen {

    private final Screen parent;
    private EditBox urlBox;
    private EditBox keyBox;
    private EditBox modelBox;
    private List<String> models = List.of();
    private String status = "";
    private int statusColor = 0xB0B0B0;

    public SetupScreen(Screen parent) {
        super(Component.literal("AI Bridge 设置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        Settings.reloadIfChanged();
        int cx = this.width / 2;
        int w = 300;
        int x = cx - w / 2;

        this.urlBox = new EditBox(this.font, x, 48, w, 20, Component.literal("服务地址"));
        this.urlBox.setHint(Component.literal("例如 http://192.168.1.5:8080"));
        this.urlBox.setValue(Settings.baseUrl);
        this.addRenderableWidget(this.urlBox);

        this.keyBox = new EditBox(this.font, x, 92, w, 20, Component.literal("API key"));
        this.keyBox.setHint(Component.literal("本地 llama-swap 留空即可"));
        this.keyBox.setValue(Settings.apiKey);
        this.addRenderableWidget(this.keyBox);

        this.modelBox = new EditBox(this.font, x, 136, w - 110, 20, Component.literal("模型"));
        this.modelBox.setHint(Component.literal("点右边查询后选择"));
        this.modelBox.setValue(Settings.model);
        this.addRenderableWidget(this.modelBox);

        this.addRenderableWidget(Button.builder(Component.literal("查询模型"), b -> fetchModels())
                .bounds(x + w - 100, 136, 100, 20).build());

        int y = 170;
        for (String m : this.models) {
            final String name = m.contains(" (") ? m.substring(0, m.indexOf(" (")) : m;
            this.addRenderableWidget(Button.builder(Component.literal("▶ " + m), b -> {
                this.modelBox.setValue(name);
                this.status = "已选中 " + name + "，别忘了点保存";
                this.statusColor = 0x90EE90;
            }).bounds(x, y, w, 20).build());
            y += 24;
        }

        this.addRenderableWidget(Button.builder(Component.literal("保存"), b -> {
            Settings.save(this.urlBox.getValue(), this.keyBox.getValue(), this.modelBox.getValue());
            this.status = "✅ 已保存，立即生效";
            this.statusColor = 0x90EE90;
        }).bounds(cx - 105, this.height - 56, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("关闭"), b -> this.onClose())
                .bounds(cx + 5, this.height - 56, 100, 20).build());
    }

    private void fetchModels() {
        this.status = "⏳ 正在查询模型列表…";
        this.statusColor = 0xB0B0B0;
        String url = this.urlBox.getValue();
        String key = this.keyBox.getValue();
        AiClient.listModels(url, key).thenAccept(list ->
                this.minecraft.execute(() -> {
                    if (list.isEmpty()) {
                        this.status = "❌ 查询失败：地址不通或不是 OpenAI 兼容接口";
                        this.statusColor = 0xFF8080;
                    } else {
                        this.models = list;
                        this.status = "找到 " + list.size() + " 个模型，点一个选中";
                        this.statusColor = 0x90EE90;
                    }
                    this.rebuildWidgets();
                }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, cx, 16, 0xFFFFFF);
        graphics.drawString(this.font, "服务地址（OpenAI 兼容，如 llama-swap）", cx - 150, 38, 0xC0C0C0);
        graphics.drawString(this.font, "API key", cx - 150, 82, 0xC0C0C0);
        graphics.drawString(this.font, "模型", cx - 150, 126, 0xC0C0C0);
        graphics.drawCenteredString(this.font, this.status, cx, this.height - 76, this.statusColor);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }
}
