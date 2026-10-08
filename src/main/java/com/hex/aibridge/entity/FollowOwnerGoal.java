package com.hex.aibridge.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.EnumSet;

public class FollowOwnerGoal extends Goal {

    private final AiNpc npc;
    private final double speed;
    private final double minDist;
    private final double maxDist;
    @Nullable
    private Player owner;

    public FollowOwnerGoal(AiNpc npc, double speed, double minDist, double maxDist) {
        this.npc = npc;
        this.speed = speed;
        this.minDist = minDist;
        this.maxDist = maxDist;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        this.owner = npc.getOwner();
        return owner != null && !owner.isSpectator() && npc.distanceToSqr(owner) > maxDist * maxDist;
    }

    @Override
    public void start() {
        npc.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.owner = null;
        npc.getNavigation().stop();
    }

    @Override
    public void tick() {
        Player o = this.owner;
        if (o == null || o.isRemoved()) return;
        double d = npc.distanceTo(o);
        if (d > 26.0) {
            npc.teleportTo(o.getX(), o.getY(), o.getZ());
            npc.getNavigation().stop();
        } else if (d > minDist) {
            npc.getNavigation().moveTo(o.getX(), o.getY(), o.getZ(), speed);
        }
    }
}
