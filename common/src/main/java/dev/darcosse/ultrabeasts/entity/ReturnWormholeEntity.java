package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.handler.VoidFallHandler;
import dev.darcosse.ultrabeasts.registry.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * The exit portal inside Ultra-Space. Entering it ends the session exactly like falling into
 * the void does.
 * <p>
 * Not persisted on purpose: the server-start clean-up removes it, and
 * {@code UltraSpaceStructureManager.ensureReturnPortal} recreates it from the position saved
 * in {@code UltraSpaceState}.
 */
public class ReturnWormholeEntity extends AbstractPortalEntity {

    /** Plane of the particle ring, relative to the entity. Mirrors the inbound wormhole. */
    private static final double PARTICLE_RING_Z_OFFSET = -1.0;

    /**
     * Where the particles converge, relative to the entity. Also the fallback of the particle
     * class, so particles spawned without a target (e.g. by /particle) look the same.
     */
    public static final double PARTICLE_DESTINATION_Z_OFFSET = 3.0;

    /** Constructor used by the entity type factory. */
    public ReturnWormholeEntity(EntityType<? extends ReturnWormholeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected SimpleParticleType particleType() {
        return ModParticles.RETURN_WORMHOLE;
    }

    @Override
    protected double particleRingZOffset() {
        return PARTICLE_RING_Z_OFFSET;
    }

    @Override
    protected double particleDestinationZOffset() {
        return PARTICLE_DESTINATION_Z_OFFSET;
    }

    /** Sends the player home and closes the portal. */
    @Override
    protected void onPlayerEnter(ServerPlayer player) {
        this.level().playSound(null, this.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);

        VoidFallHandler.handleUltraBeastVoidFall(player);

        this.discard();
    }

    /** Nothing to read: the portal position lives in {@code UltraSpaceState}. */
    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
    }

    /** Nothing to write: see {@link #readAdditionalSaveData}. */
    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
    }
}
