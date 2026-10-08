package com.hex.aibridge;

import com.hex.aibridge.entity.AiNpc;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AiCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ai")
                .then(Commands.literal("models")
                        .requires(s -> s.hasPermission(2))
                        .executes(AiCommand::models))
                .then(Commands.literal("summon")
                        .requires(s -> s.hasPermission(2))
                        .executes(AiCommand::summon))
                .then(Commands.literal("dismiss")
                        .requires(s -> s.hasPermission(2))
                        .executes(AiCommand::dismiss))
                .then(Commands.argument("question", MessageArgument.message())
                        .executes(AiCommand::ask)));
    }

    private static int ask(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String question = MessageArgument.getMessage(ctx, "question").getString();
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("🤔 AI 思考中…").withStyle(ChatFormatting.GRAY), false);
        List<AiClient.Message> messages = new ArrayList<>();
        messages.add(new AiClient.Message("system", systemPrompt()));
        messages.add(new AiClient.Message("user", question));
        AiClient.ask(messages, Config.MAX_TOKENS.get())
                .thenAccept(reply -> src.getServer().execute(() -> deliver(src, reply)));
        return 1;
    }

    static void deliver(CommandSourceStack src, AiClient.Reply reply) {
        for (String chunk : splitChunks(reply.text(), 480)) {
            src.sendSystemMessage(Component.literal(chunk));
        }
        for (String cmd : reply.commands()) {
            runWhitelisted(src, cmd);
        }
    }

    public static void runWhitelisted(CommandSourceStack src, String cmd) {
        String head = cmd.split("\\s+")[0].toLowerCase(Locale.ROOT);
        boolean allowed = Config.COMMAND_WHITELIST.get().stream().anyMatch(w -> w.equalsIgnoreCase(head));
        if (!allowed) {
            src.sendSystemMessage(Component.literal("⛔ 白名单拦截: " + cmd).withStyle(ChatFormatting.RED));
            return;
        }
        try {
            src.getServer().getCommands()
                    .performPrefixedCommand(src.getServer().createCommandSourceStack(), cmd);
            src.sendSystemMessage(Component.literal("🤖 已执行 `" + cmd + "`").withStyle(ChatFormatting.GREEN));
        } catch (Exception e) {
            src.sendSystemMessage(Component.literal("❌ 执行失败: " + cmd + " — " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static int summon(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        Entity executor = ctx.getSource().getEntity();
        AiNpc npc = new AiNpc(EntityInit.NPC.get(), level);
        npc.moveTo(ctx.getSource().getPosition(),
                executor != null ? executor.getYRot() : 0.0f, 0.0f);
        if (executor != null) {
            npc.setOwnerUUID(executor.getUUID());
        }
        level.addFreshEntity(npc);
        ctx.getSource().sendSuccess(
                () -> Component.literal("✨ 已召唤 AI 同伴「小蒸」，它会跟着你并主动搭话。")
                        .withStyle(ChatFormatting.AQUA), true);
        return 1;
    }

    private static int dismiss(CommandContext<CommandSourceStack> ctx) {
        Entity executor = ctx.getSource().getEntity();
        AiNpc npc = AiNpc.findNearest(ctx.getSource().getLevel(),
                executor != null ? executor.position() : ctx.getSource().getPosition(), 32.0);
        if (npc == null) {
            ctx.getSource().sendFailure(Component.literal("附近没有 AI 同伴。"));
            return 0;
        }
        npc.discard();
        ctx.getSource().sendSuccess(() -> Component.literal("👋 AI 同伴已离开。").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int models(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        src.sendSuccess(() -> Component.literal("📡 查询模型列表…").withStyle(ChatFormatting.GRAY), false);
        AiClient.listModels().thenAccept(models -> src.getServer().execute(() -> {
            if (models.isEmpty()) {
                src.sendSystemMessage(Component.literal("没有可用模型（服务没开机？）。"));
            } else {
                for (String m : models) {
                    src.sendSystemMessage(Component.literal("  • " + m));
                }
            }
        }));
        return 1;
    }

    static String systemPrompt() {
        return Config.SYSTEM_PROMPT.get().replace("{whitelist}", Config.whitelistCsv());
    }

    static List<String> splitChunks(String text, int size) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        StringBuilder cur = new StringBuilder();
        for (String line : text.replace("\r\n", "\n").split("\n")) {
            if (line.length() > size) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
                for (int i = 0; i < line.length(); i += size) {
                    out.add(line.substring(i, Math.min(line.length(), i + size)));
                }
            } else if (cur.length() + line.length() + 1 > size) {
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(line);
            } else {
                if (cur.length() > 0) cur.append('\n');
                cur.append(line);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
