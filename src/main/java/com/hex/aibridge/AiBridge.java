package com.hex.aibridge;

import com.hex.aibridge.entity.AiNpc;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

@Mod(AiBridge.MODID)
public class AiBridge {

    public static final String MODID = "aibridge";

    public AiBridge(IEventBus modBus, ModContainer container) {
        Config.register(container);
        EntityInit.init(modBus);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerChat);
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            com.hex.aibridge.client.ClientEvents.init();
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        AiCommand.register(event.getDispatcher());
    }

    private void onServerChat(ServerChatEvent event) {
        if (!Config.NPC_CHAT_REACT.get()) return;
        net.minecraft.server.level.ServerLevel level =
                (net.minecraft.server.level.ServerLevel) event.getPlayer().level();
        AiNpc npc = AiNpc.findNearest(level, event.getPlayer(), 16.0);
        if (npc != null) {
            npc.reactTo(level, event.getPlayer(), event.getMessage().getString());
        }
    }
}
