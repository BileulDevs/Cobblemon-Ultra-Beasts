package dev.darcosse.ultrabeasts.fabric.entity;

import dev.darcosse.ultrabeasts.fabric.handler.VoidFallHandler;
import dev.darcosse.ultrabeasts.fabric.registry.ModParticles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class ReturnWormholeEntity extends Entity {

    public ReturnWormholeEntity(EntityType<? extends ReturnWormholeEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            summonPortal();
        }
    }

    private void summonPortal() {
        if (!(this.getWorld() instanceof ServerWorld world)) return;

        int numberOfParticles = 50;

        double radius = 2;

        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ() - 1;

        for (int i = 0; i < numberOfParticles; i++) {
            double angle = world.random.nextDouble() * 2 * Math.PI;

            double startX = centerX + Math.cos(angle) * radius;
            double startY = centerY + Math.sin(angle) * radius;
            double startZ = centerZ;

            world.spawnParticles(
                    ModParticles.RETURN_WORMHOLE,
                    startX, startY, startZ,
                    0,
                    centerX,
                    centerY,
                    centerZ,
                    0.0
            );
        }
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            teleportBack(serverPlayer);
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            teleportBack(serverPlayer);
        }
        super.onPlayerCollision(player);
    }

    private void teleportBack(ServerPlayerEntity player) {
        if (this.isRemoved()) return;

        this.getWorld().playSound(null, this.getBlockPos(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 1.0f);

        VoidFallHandler.handleUltraBeastVoidFall(player);

        this.discard();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}