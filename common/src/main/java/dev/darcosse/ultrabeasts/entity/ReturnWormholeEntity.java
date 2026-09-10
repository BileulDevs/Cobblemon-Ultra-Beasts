package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.UltraBeasts;
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

    /** See WormholeEntity.summonPortal: one packet per particle. */
    private static final int PARTICLES_PER_TICK = 18;
    private static final double PORTAL_RADIUS = 2.0;

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

        if (this.level() instanceof ServerLevel level) {
            summonPortal(level);
        }
    }


    /**
     * Set once if the particle type never resolves client-side.
     *
     * Each particle is sent with count 0 so its velocity fields can carry the
     * destination, which means one packet per particle. If the client is
     * missing the mod's resources it logs a warning for every single one:
     * 18 per tick per portal is 360 lines a second, enough to stall the client
     * and bury anything useful in the log.
     */
    private boolean particlesUnavailable = false;

    private void summonPortal(ServerLevel level) {
        if (particlesUnavailable) return;

        if (ModParticles.RETURN_WORMHOLE == null) {
            particlesUnavailable = true;
            UltraBeasts.LOGGER.warn(
                    "Particle type is not registered, disabling portal particles for this entity.");
            return;
        }

        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ() - 1;

        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;

            level.sendParticles(
                    ModParticles.RETURN_WORMHOLE,
                    centerX + Math.cos(angle) * PORTAL_RADIUS,
                    centerY + Math.sin(angle) * PORTAL_RADIUS,
                    centerZ,
                    0,
                    centerX, centerY, centerZ,
                    0.0
            );
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        teleportBack(serverPlayer);
        return InteractionResult.SUCCESS;
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
