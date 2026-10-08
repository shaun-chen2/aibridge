# AI Bridge — NeoForge 1.21.1 Mod

[中文说明](README.md)

Connect a local AI (llama-swap / any OpenAI-compatible endpoint) to Minecraft.

## Install
Drop `aibridge-1.0.0.jar` into your instance's `mods/` folder (requires NeoForge 21.1.x / MC 1.21.1).
An advanced config file `config/aibridge-common.toml` is generated on first launch.

## In-game commands
| Command | Description |
|---------|-------------|
| `/ai setup` | **Opens the settings GUI**: enter the server URL and API key, click "查询模型" (Fetch models) to list models from the server, click one to select, then Save (takes effect immediately) |
| `/ai <question>` | Ask the AI; the answer appears in chat (available to everyone) |
| `/ai summon` | Summon the AI companion "Xiaozheng" (OP required) |
| `/ai dismiss` | Dismiss the nearest AI companion (OP required) |
| `/ai models` | Quickly list models and their load status in chat (OP required) |

There are no built-in defaults — run `/ai setup` first. Until configured, `/ai` shows a hint instead of connecting blindly.
Settings are stored in `config/aibridge-settings.json` (read/written by the GUI, auto-reloaded before every request).

## The AI companion "Xiaozheng"
- Follows you around (teleports if left too far behind) and proactively comments on the situation (time of day, weather, your health, biome) — one sentence shown above its head, hidden after 6 seconds.
- Talks every 15 seconds by default; adjust with `npcTalkIntervalSec`.
- Want it to react to what you say in chat? Set `npcChatReact = true` (sends every chat message to the AI — costs compute).

## AI changes the game (whitelisted commands)
If the AI's reply contains a line like `!CMD give @p diamond 64`, the mod validates it against the whitelist and executes it with console permissions.
Default whitelist: `give, time, weather, tp, teleport, effect, xp, gamemode` (configurable).
Anything outside the whitelist is blocked with a warning.

## Config highlights (config/aibridge-common.toml, advanced)
- `timeoutMs = 120000` — cold-starting a large model can take a minute or two; don't set this too low
- `systemPrompt` / `npcPrompt` — prompt templates; `{whitelist}` and `{owner}` are substituted
- `commandWhitelist` — commands the AI is allowed to run
- `npcTalkIntervalSec` / `npcChatReact` / `npcMaxSpeechLen` — companion behavior

## Notes
- All AI requests are asynchronous — the game thread never blocks.
- If the AI server is unreachable, `/ai` fails with a clear error instead of hanging.
- Requests send `User-Agent: curl/8.7.1` (llama-swap returns 503 for some default user agents).

## Rebuild
```bash
cd aibridge
GRADLE_USER_HOME="$PWD/.gradle-home" gradle build
# output: build/libs/aibridge-1.0.0.jar
```
