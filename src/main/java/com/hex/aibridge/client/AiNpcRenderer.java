package com.hex.aibridge.client;

import com.hex.aibridge.entity.AiNpc;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class AiNpcRenderer extends MobRenderer<AiNpc, HumanoidModel<AiNpc>> {

    public AiNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(AiNpc entity) {
        return ResourceLocation.withDefaultNamespace("textures/entity/steve.png");
    }
}
