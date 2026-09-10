package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.handler.VoidFallHandler;
import dev.darcosse.ultrabeasts.registry.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ReturnWormholeEntity extends Entity {

    public ReturnWormholeEntity(EntityType<? extends ReturnWormholeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            summonPortal();
        }
    }

    private void summonPortal() {
        if (!(this.level() instanceof ServerLevel level)) return;

        int numberOfParticles = 50;

        double radius = 2;

        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ() - 1;

        for (int i = 0; i < numberOfParticles; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;

            double startX = centerX + Math.cos(angle) * radius;
            double startY = centerY + Math.sin(angle) * radius;
            double startZ = centerZ;

            level.sendParticles(
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
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            teleportBack(serverPlayer);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            teleportBack(serverPlayer);
        }
        super.playerTouch(player);
    }

    private void teleportBack(ServerPlayer player) {
        if (this.isRemoved()) return;

        this.level().playSound(null, this.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);

        VoidFallHandler.handleUltraBeastVoidFall(player);

        this.discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
    }
}
