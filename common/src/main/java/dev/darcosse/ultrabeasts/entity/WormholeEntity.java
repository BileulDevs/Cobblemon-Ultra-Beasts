package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.dimension.ReturnPointStore;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.handler.UltraSpaceSession;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import dev.darcosse.ultrabeasts.registry.ModEntities;
import dev.darcosse.ultrabeasts.registry.ModEvents;
import dev.darcosse.ultrabeasts.registry.ModParticles;
import dev.darcosse.ultrabeasts.util.ServerScheduler;
import dev.darcosse.ultrabeasts.util.TeleportEffects;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

/**
 * The inbound wormhole that appears in the overworld and carries one player into
 * Ultra-Space.
 * <p>
 * Also owns the random spawn logic: where a wormhole may appear and when. The visible
 * arrival is played by {@link WormholeSpawnAnimation}, which spawns this entity at its end.
 */
public class WormholeEntity extends AbstractPortalEntity {

    /** How long a wormhole stays open, in ticks (60 s). */
    private static final int LIFESPAN_TICKS = 1200;

    // ---- spawn placement -------------------------------------------------

    /** Inner radius of the ring around the player the portal can appear in, in blocks. */
    private static final int MIN_DISTANCE = 6;

    /** Outer radius of that ring, in blocks. Larger makes the portal harder to notice. */
    private static final int SEARCH_RADIUS = 20;

    /** Candidate positions tried per spawn. Each costs a heightmap read and a few block reads. */
    private static final int SPAWN_ATTEMPTS = 40;

    /**
     * Height of the portal above the ground.
     * <p>
     * The renderer draws the quad at scale 2 centred on the entity, so at 1 the portal rests
     * on the ground and its hitbox overlaps a standing player: you walk straight into it. At
     * 2 or 3 it floats, and can only be entered by clicking it.
     */
    private static final int HEIGHT_ABOVE_GROUND = 3;

    /** Air blocks required above the portal. */
    private static final int CLEARANCE = 3;

    /** Reject ground more than this far above or below the player. */
    private static final int MAX_GROUND_OFFSET = 10;

    /** Plane of the particle ring, relative to the entity. Mirrors the exit portal. */
    private static final double PARTICLE_RING_Z_OFFSET = 1.0;

    /**
     * Where the particles converge, relative to the entity. Also the fallback of the particle
     * class, so particles spawned without a target (e.g. by /particle) look the same.
     */
    public static final double PARTICLE_DESTINATION_Z_OFFSET = -3.0;

    /** Radius within which the nearest player is told a wormhole opened. */
    private static final double SPAWN_MESSAGE_RADIUS = 64.0;

    /** Delay before the arrival ambience plays, in ticks: lets the destination chunks load. */
    private static final int ARRIVAL_SOUND_DELAY = 10;

    /** Advancement granted on the first trip, entirely, whatever its criteria. */
    private static final ResourceLocation ENTER_ULTRA_SPACE_ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "enter_ultra_space_dimension");

    /** NBT key of {@link #placementTime}. Renaming it resets the lifespan of saved wormholes. */
    private static final String NBT_PLACEMENT_TIME = "PlacementTime";

    /**
     * Game tick this wormhole was placed. Instance field, not static: it is written into this
     * entity's NBT, and a static value meant every wormhole shared, and overwrote, the same
     * timestamp.
     */
    private long placementTime;

    /** Constructor used by the entity type factory. */
    public WormholeEntity(EntityType<? extends WormholeEntity> type, Level level) {
        super(type, level);
    }

    /** Records when the wormhole opened. Called right after spawning, by the animation entity. */
    public void setPlacementTime(long tick) {
        this.placementTime = tick;
    }

    @Override
    protected SimpleParticleType particleType() {
        return ModParticles.WORMHOLE;
    }

    @Override
    protected double particleRingZOffset() {
        return PARTICLE_RING_Z_OFFSET;
    }

    @Override
    protected double particleDestinationZOffset() {
        return PARTICLE_DESTINATION_Z_OFFSET;
    }

    /** Closes the wormhole once its lifespan is over. */
    @Override
    protected void serverTick(ServerLevel level) {
        if (placementTime == 0) {
            // Spawned without an explicit timestamp (command, or loaded from an older save):
            // start the clock now rather than expiring instantly.
            placementTime = level.getGameTime();
        }

        if (level.getGameTime() - placementTime >= LIFESPAN_TICKS) {
            this.discard();
        }
    }

    /**
     * Whether a wormhole is present in the level, or about to be (animation running).
     * <p>
     * Derived from the world instead of a cached static reference: the previous version kept
     * activeWormhole/isSpawning in static fields, which survived world reloads in single
     * player and were shared across dimensions.
     */
    public static boolean hasActiveWormhole(ServerLevel level) {
        if (!level.getEntities(ModEntities.WORMHOLE, e -> !e.isRemoved()).isEmpty()) {
            return true;
        }
        return !level.getEntities(ModEntities.WORMHOLE_ANIMATION, e -> !e.isRemoved()).isEmpty();
    }

    /**
     * Opens a wormhole on the player, bypassing the random roll. Backs
     * {@code /ultrabeasts wormhole spawn}.
     *
     * @return false if Ultra-Space is already claimed, in which case nothing is spawned
     */
    public static boolean forceSpawnToPlayer(ServerLevel level, ServerPlayer player) {
        // The command used to bypass every check, which let an op drop a second player into an
        // Ultra-Space that was already taken.
        if (UltraSpaceSession.isOccupied(level.getServer())) {
            return false;
        }

        ModEvents.cleanUp(level.getServer());
        spawnWormhole(level, player.blockPosition());
        return true;
    }

    /**
     * One random spawn attempt. Cheap checks run first, so the usual outcome (the roll fails)
     * costs almost nothing.
     */
    public static void tryRandomSpawn(ServerLevel level, Random random) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        if (level.players().isEmpty()) return;
        if (hasActiveWormhole(level)) return;

        // A claimed dimension holds back new wormholes, even if the occupant is currently
        // offline: that is what protects their session.
        if (UltraSpaceSession.isOccupied(level.getServer())) return;

        if (random.nextInt(ConfigManager.getWormholeSpawnChance()) != 0) return;

        BlockPos spawnPos = findValidSpawnLocation(level, random);
        if (spawnPos != null) {
            spawnWormhole(level, spawnPos);
        }
    }

    /**
     * Picks a spot near a random player, or returns null if none of the attempts fits.
     * <p>
     * Uses MOTION_BLOCKING_NO_LEAVES rather than WORLD_SURFACE: the latter counts leaves and
     * snow layers, which put the portal above the treetops of a spruce forest, fifteen or more
     * blocks off the ground. The candidate is also compared to the player's own altitude, so
     * on broken terrain the portal does not appear on a neighbouring cliff top.
     */
    private static BlockPos findValidSpawnLocation(ServerLevel level, Random random) {
        var players = level.players();
        if (players.isEmpty()) return null;

        BlockPos playerPos = players.get(random.nextInt(players.size())).blockPosition();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
            // Polar sampling: an even spread through the ring, instead of the corner bias a
            // random x/z offset produces.
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = MIN_DISTANCE + random.nextDouble() * (SEARCH_RADIUS - MIN_DISTANCE);

            int x = playerPos.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = playerPos.getZ() + (int) Math.round(Math.sin(angle) * distance);

            cursor.set(x, playerPos.getY(), z);

            // Never force a chunk to load just to look for a spawn.
            if (!level.hasChunkAt(cursor)) continue;

            int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            // Stay at the player's level: no cliff tops, no ravine floors.
            if (Math.abs(groundY - playerPos.getY()) > MAX_GROUND_OFFSET) continue;

            BlockPos spawnPos = new BlockPos(x, groundY + HEIGHT_ABOVE_GROUND, z);

            if (isValidSpawnLocation(level, spawnPos)) {
                return spawnPos;
            }
        }

        return null;
    }

    /** Whether the portal has clear room and something solid and dry under it. */
    private static boolean isValidSpawnLocation(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // The portal renders two blocks tall, centred on the entity, so check one below as
        // well as the headroom above.
        for (int i = -1; i <= CLEARANCE; i++) {
            cursor.set(pos.getX(), pos.getY() + i, pos.getZ());
            if (!level.getBlockState(cursor).isAir()) return false;
        }

        // Ground underneath, and not a lake or a lava pool.
        cursor.set(pos.getX(), pos.getY() - HEIGHT_ABOVE_GROUND - 1, pos.getZ());
        var ground = level.getBlockState(cursor);

        if (ground.isAir()) return false;
        return ground.getFluidState().isEmpty();
    }

    /** Starts the arrival animation; the wormhole itself appears when the animation ends. */
    private static void spawnWormhole(ServerLevel level, BlockPos pos) {
        level.getEntities(ModEntities.WORMHOLE, e -> true).forEach(Entity::discard);

        WormholeSpawnAnimation animation = new WormholeSpawnAnimation(
                ModEntities.WORMHOLE_ANIMATION, level, pos);
        animation.setPos(pos.getX() + 0.5, pos.getY() + WormholeSpawnAnimation.DESCENT_HEIGHT, pos.getZ() + 0.5);
        level.addFreshEntity(animation);

        level.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.5f);

        if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SPAWN_MESSAGE_RADIUS, false)
                instanceof ServerPlayer nearest) {
            nearest.displayClientMessage(Component.translatable("message.ultrabeasts.portal_spawn"), false);
        }
    }

    /** Sends the player into Ultra-Space if the dimension is free, then closes the wormhole. */
    @Override
    protected void onPlayerEnter(ServerPlayer player) {
        if (!UltraSpaceSession.canEnter(player)) {
            player.displayClientMessage(
                    Component.translatable("message.ultrabeasts.occupied"), true);
            return;
        }

        this.level().playSound(null, this.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);

        teleportToUltraSpaceDimension(player);

        this.discard();
    }

    /** Builds a random structure in Ultra-Space and moves the player into it. */
    private void teleportToUltraSpaceDimension(ServerPlayer player) {
        ServerLevel ultraSpace = player.getServer().getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace == null) {
            UltraBeasts.LOGGER.error("Ultra-Space dimension is not loaded, aborting teleport.");
            return;
        }

        String structureKey = UltraSpaceStructureManager.getRandomStructureKey(player.getRandom());
        UltraSpaceStructureManager.StructureConfig config = UltraSpaceStructureManager.getConfig(structureKey);

        // Checked BEFORE claiming the dimension and saving the return point: returning after
        // those two would leave Ultra-Space claimed by a player who never entered it.
        if (config == null) {
            UltraBeasts.LOGGER.error("Could not find the config for {}", structureKey);
            return;
        }

        if (player.level().dimension().equals(Level.OVERWORLD)) {
            ReturnPointStore.get(player.getServer()).put(player.getUUID(), player.blockPosition());
        }

        UltraSpaceSession.claim(player);

        UltraSpaceStructureManager.killAllPokemonOfWorld(ultraSpace);
        grantEnterAdvancement(player);
        // Also spawns the exit portal and records its position, so it can be restored after
        // a restart.
        UltraSpaceStructureManager.placeStructure(ultraSpace, structureKey);

        TeleportEffects.applyTransitionBlindness(player);

        BlockPos playerSpawn = config.playerSpawn();
        player.teleportTo(ultraSpace,
                playerSpawn.getX() + 0.5, playerSpawn.getY(), playerSpawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());

        ServerScheduler.schedule(ARRIVAL_SOUND_DELAY, () -> {
            // The player may have left, died or logged out during the delay.
            if (!player.isAlive() || player.isRemoved()) return;
            if (!player.level().dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) return;

            BlockPos pos = player.blockPosition();
            ultraSpace.playSound(null, pos, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 2.0f, 1.0f);
            ultraSpace.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.5f, 0.8f);

            player.displayClientMessage(Component.translatable("dimension.travel.ultra_space"), false);
        });
    }

    /** Completes the "Into the Unknown" advancement, whatever criteria it declares. */
    private static void grantEnterAdvancement(ServerPlayer player) {
        AdvancementHolder advancement = player.getServer().getAdvancements().get(ENTER_ULTRA_SPACE_ADVANCEMENT);
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) return;

        // getRemainingCriteria() returns a fresh list, so awarding inside the loop is safe.
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.placementTime = nbt.getLong(NBT_PLACEMENT_TIME);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putLong(NBT_PLACEMENT_TIME, this.placementTime);
    }
}
