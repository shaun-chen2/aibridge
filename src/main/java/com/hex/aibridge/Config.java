package com.hex.aibridge;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class Config {

    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue TIMEOUT_MS = B
            .comment("单次请求超时（毫秒）")
            .defineInRange("timeoutMs", 120000, 5000, 600000);

    public static final ModConfigSpec.IntValue MAX_TOKENS = B
            .comment("/ai 聊天回答的最大 token")
            .defineInRange("maxTokens", 800, 64, 8192);

    public static final ModConfigSpec.DoubleValue TEMPERATURE = B
            .defineInRange("temperature", 0.7, 0.0, 2.0);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> COMMAND_WHITELIST = B
            .comment("AI 可执行的游戏指令白名单（按指令第一个词匹配）")
            .defineList("commandWhitelist",
                    List.of("give", "time", "weather", "tp", "teleport", "effect", "xp", "gamemode"),
                    s -> s instanceof String);

    public static final ModConfigSpec.ConfigValue<String> SYSTEM_PROMPT = B
            .comment("/ai 聊天的系统提示词，{whitelist} 会替换成白名单")
            .define("systemPrompt",
                    "你是 Minecraft 游戏里的 AI 助手。用简体中文简洁回答，不超过300字，不要用 markdown 格式。"
                            + "如果玩家希望你改变游戏，在回复最后单独一行写：!CMD <游戏指令>。"
                            + "指令只允许以下列单词开头：{whitelist}。玩家名用 @a、@p 或具体名字。没有把握就不要输出 !CMD 行。");

    public static final ModConfigSpec.ConfigValue<String> NPC_PROMPT = B
            .comment("NPC 同伴主动搭话的提示词，{owner} 替换为玩家名，{whitelist} 替换成白名单")
            .define("npcPrompt",
                    "你是玩家 {owner} 的 AI 同伴，名字叫小蒸，陪在 TA 身边玩 Minecraft。"
                            + "根据下面的环境信息，用一句简短中文（不超过40字）主动搭话：可以闲聊、提醒危险、给建议、吐槽。"
                            + "如需改变游戏，最后单独一行写 !CMD <指令>（白名单：{whitelist}）。只输出这句话本身，不要引号。");

    public static final ModConfigSpec.BooleanValue NPC_CHAT_REACT = B
            .comment("true 时 NPC 会回应玩家在游戏聊天栏说的话（每条都会问一次 AI，费算力，默认关）")
            .define("npcChatReact", false);

    public static final ModConfigSpec.IntValue NPC_TALK_INTERVAL_SEC = B
            .comment("NPC 主动搭话间隔（秒）")
            .defineInRange("npcTalkIntervalSec", 15, 5, 600);

    public static final ModConfigSpec.IntValue NPC_MAX_SPEECH_LEN = B
            .comment("NPC 单句话最大字数")
            .defineInRange("npcMaxSpeechLen", 60, 10, 200);

    public static final ModConfigSpec SPEC = B.build();

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public static String whitelistCsv() {
        return String.join(", ", COMMAND_WHITELIST.get());
    }
}
