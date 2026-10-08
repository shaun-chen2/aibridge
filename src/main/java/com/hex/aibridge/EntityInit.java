package com.hex.aibridge;

import com.hex.aibridge.client.AiNpcRenderer;
import com.hex.aibridge.entity.AiNpc;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EntityInit {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AiBridge.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AiNpc>> NPC =
            ENTITY_TYPES.register("ai_npc",
                    () -> EntityType.Builder.of(AiNpc::new, MobCategory.CREATURE)
                            .sized(0.6f, 1.9f)
                            .build("aibridge:ai_npc"));

    public static void init(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) ->
                event.put(NPC.get(), AiNpc.createAttributes().build()));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerEntityRenderer(NPC.get(), AiNpcRenderer::new));
    }
}
