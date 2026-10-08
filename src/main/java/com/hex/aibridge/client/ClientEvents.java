package com.hex.aibridge.client;

import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

public class ClientEvents {

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientCommands);
    }

    private static void onClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ai")
                .then(Commands.literal("setup")
                        .executes(ctx -> {
                            net.minecraft.client.Minecraft.getInstance().setScreen(new SetupScreen(null));
                            return 1;
                        })));
    }
}
