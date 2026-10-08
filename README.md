# AI Bridge（AI 桥）— NeoForge 1.21.1 模组

把本地 AI（llama-swap / 任何 OpenAI 兼容接口）接进 Minecraft。

## 安装
把 `aibridge-1.0.0.jar` 放进实例的 `mods/` 文件夹（需要 NeoForge 21.1.x / MC 1.21.1）。
首次启动后生成配置：`config/aibridge-common.toml`。

## 游戏内命令
| 命令 | 作用 |
|------|------|
| `/ai setup` | **打开设置界面**：填服务地址、API key，点「查询模型」列出服务器上的模型，点选后保存（即改即生效） |
| `/ai <任意问题>` | 问 AI，回答显示在聊天栏（所有人可用） |
| `/ai summon` | 召唤 AI 同伴「小蒸」（需 OP） |
| `/ai dismiss` | 收起附近的 AI 同伴（需 OP） |
| `/ai models` | 聊天栏里快速列出模型和加载状态（需 OP） |

没有内置默认地址——第一次用先 `/ai setup` 配置，未配置时 `/ai` 会提示而不是瞎连。
设置存在 `config/aibridge-settings.json`（GUI 读写，每次请求前自动重载）。

## AI 同伴「小蒸」
- 跟着你走（太远自动传送），会主动根据环境搭话（时间/天气/血量/生物群系），头顶显示一句话，6 秒后收起。
- 默认每 15 秒搭话一次，可在配置里调 `npcTalkIntervalSec`。
- 想要它回应聊天栏里你说的话：`npcChatReact = true`（每条都问一次 AI，费算力）。

## AI 改变游戏（白名单指令）
AI 回复里如果有一行 `!CMD give @p diamond 64`，模组会校验白名单后用控制台权限执行。
默认白名单：`give, time, weather, tp, teleport, effect, xp, gamemode`（配置里可改）。
白名单外的指令一律拦截并提示。

## 配置要点（config/aibridge-common.toml，进阶项）
- `timeoutMs = 120000` — 冷启动加载大模型可能要一两分钟，别调太小
- `systemPrompt` / `npcPrompt` — 提示词，`{whitelist}`、`{owner}` 会被替换
- `commandWhitelist` — AI 可执行的指令白名单
- `npcTalkIntervalSec` / `npcChatReact` / `npcMaxSpeechLen` — NPC 行为

## 注意
- 所有 AI 请求都是异步的，不会卡游戏主线程。
- 服务器（192.168.31.136）没开机时，`/ai` 会明确报错而不是卡死。
- 请求带 `User-Agent: curl/8.7.1`（llama-swap 对某些默认 UA 会 503）。

## 重新构建
```bash
cd aibridge
GRADLE_USER_HOME="$PWD/.gradle-home" gradle build
# 产物：build/libs/aibridge-1.0.0.jar
```
