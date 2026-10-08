package com.hex.aibridge.entity;

import com.hex.aibridge.AiClient;
import com.hex.aibridge.AiCommand;
import com.hex.aibridge.Config;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class AiNpc extends PathfinderMob {

    @Nullable
    private UUID ownerUUID;
    private long lastTalkGameTime = Long.MIN_VALUE;
    private boolean talking = false;
    private long speechClearAt = 0L;

    public AiNpc(EntityType<? extends AiNpc> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.25, 3.0, 12.0));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    public void setOwnerUUID(UUID id) {
        this.ownerUUID = id;
    }

    @Nullable
    public Player getOwner() {
        if (ownerUUID == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getPlayerByUUID(ownerUUID);
    }

    @Nullable
    public static AiNpc findNearest(ServerLevel level, Vec3 pos, double maxDist) {
        AiNpc best = null;
        double bestD = maxDist * maxDist;
        for (AiNpc npc : level.getEntitiesOfClass(AiNpc.class,
                new net.minecraft.world.phys.AABB(pos.add(-maxDist, -maxDist, -maxDist),
                        pos.add(maxDist, maxDist, maxDist)))) {
            double d = npc.distanceToSqr(pos);
            if (d < bestD) {
                bestD = d;
                best = npc;
            }
        }
        return best;
    }

    @Nullable
    public static AiNpc findNearest(ServerLevel level, Entity center, double maxDist) {
        return findNearest(level, center.position(), maxDist);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel sl)) return;
        if (speechClearAt != 0 && sl.getGameTime() >= speechClearAt) {
            speechClearAt = 0;
            this.setCustomName(null);
        }
        if (talking) return;
        if (!com.hex.aibridge.Settings.configured()) return;
        long interval = Config.NPC_TALK_INTERVAL_SEC.get() * 20L;
        if (sl.getGameTime() - lastTalkGameTime < interval) return;
        Player owner = getOwner();
        if (owner == null || owner.isSpectator()) return;
        if (this.distanceToSqr(owner) > 24.0 * 24.0) return;
        talk(sl, owner, null);
    }

    public void talk(ServerLevel sl, Player owner, @Nullable String playerWords) {
        talking = true;
        lastTalkGameTime = sl.getGameTime();
        String prompt = Config.NPC_PROMPT.get()
                .replace("{owner}", owner.getName().getString())
                .replace("{whitelist}", Config.whitelistCsv());
        String context = buildContext(sl, owner);
        if (playerWords != null) {
            context += "\n玩家刚刚在聊天里说：" + playerWords + "\n请回应 TA。";
        }
        AiClient.ask(List.of(new AiClient.Message("system", prompt),
                        new AiClient.Message("user", context)), 200)
                .thenAccept(reply -> sl.getServer().execute(() -> {
                    talking = false;
                    if (!this.isAlive()) return;
                    String text = reply.text().isBlank() ? "……" : reply.text().replaceAll("[\"“”]", "");
                    int max = Config.NPC_MAX_SPEECH_LEN.get();
                    if (text.length() > max) text = text.substring(0, max) + "…";
                    say(sl, text);
                    for (String cmd : reply.commands()) {
                        AiCommand.runWhitelisted(owner.createCommandSourceStack(), cmd);
                    }
                }));
    }

    public void reactTo(ServerLevel sl, Player player, String words) {
        if (talking) return;
        talk(sl, player, words);
    }

    private void say(ServerLevel sl, String text) {
        this.setCustomName(Component.literal(text));
        this.setCustomNameVisible(true);
        this.speechClearAt = sl.getGameTime() + 120;
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(),
                4, 0.2, 0.15, 0.2, 0.0);
    }

    private String buildContext(ServerLevel sl, Player owner) {
        String time = (sl.getDayTime() % 24000L < 12000L) ? "白天" : "夜晚";
        String biome = sl.getBiome(owner.blockPosition()).unwrapKey()
                .map(k -> k.location().getPath()).orElse("未知");
        return "玩家名：" + owner.getName().getString()
                + "；维度：" + sl.dimension().location()
                + "；生物群系：" + biome
                + "；时间：" + time
                + "；天气：" + (sl.isRaining() ? "下雨" : "晴")
                + "；玩家血量：" + Mth.ceil(owner.getHealth()) + "/20"
                + "；手持物品：" + (owner.getMainHandItem().isEmpty()
                        ? "空手" : owner.getMainHandItem().getItem().getDescriptionId());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUUID != null) {
            tag.putUUID("AiOwner", ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("AiOwner")) {
            ownerUUID = tag.getUUID("AiOwner");
        }
    }
}
