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
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Random;

public class WormholeEntity extends Entity {

    private static final int LIFESPAN_TICKS = 1200;

    // ---- spawn placement -------------------------------------------------

    /** Ring around the player the portal can appear in, in blocks. */
    private static final int MIN_DISTANCE = 6;
    private static final int SEARCH_RADIUS = 20;

    private static final int SPAWN_ATTEMPTS = 40;

    /**
     * Height of the portal above the ground.
     *
     * The renderer draws the quad at scale 2 centred on the entity, so at 1 the
     * portal visually rests on the ground and its hitbox overlaps a standing
     * player: you walk straight into it. Raise it to 2 or 3 for a floating look,
     * but then it can only be entered by clicking on it.
     */
    private static final int HEIGHT_ABOVE_GROUND = 3;

    /** Air blocks required above the portal. */
    private static final int CLEARANCE = 3;

    /** Reject ground more than this far above or below the player. */
    private static final int MAX_GROUND_OFFSET = 10;

    /** Particles emitted per tick. Each one is its own packet — see summonPortal. */
    private static final int PARTICLES_PER_TICK = 18;
    private static final double PORTAL_RADIUS = 2.0;

    /** Delay before the arrival ambience plays, in ticks. */
    private static final int ARRIVAL_SOUND_DELAY = 10;

    /**
     * Tick this wormhole was placed. Instance field, not static: it is written
     * into this entity's NBT, so a static value meant every wormhole shared —
     * and overwrote — the same timestamp.
     */
    private long placementTime;

    public WormholeEntity(EntityType<? extends WormholeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    /** Called right after spawning, by the animation entity. */
    public void setPlacementTime(long tick) {
        this.placementTime = tick;
    }

    @Override
    public void tick() {
        super.tick();

        if (!(this.level() instanceof ServerLevel level)) return;

        if (placementTime == 0) {
            // Spawned without an explicit timestamp (command, or loaded from an
            // older save): start the clock now rather than expiring instantly.
            placementTime = level.getGameTime();
        }

        if (level.getGameTime() - placementTime >= LIFESPAN_TICKS) {
            this.discard();
            return;
        }

        summonPortal(level);
    }

    /**
     * Is a wormhole present, or about to be?
     *
     * Derived from the world instead of a cached static reference: the previous
     * version kept activeWormhole/isSpawning in static fields, which survived
     * world reloads in single player and were shared across dimensions.
     */
    public static boolean hasActiveWormhole(ServerLevel level) {
        if (!level.getEntities(ModEntities.WORMHOLE, e -> !e.isRemoved()).isEmpty()) {
            return true;
        }
        return !level.getEntities(ModEntities.WORMHOLE_ANIMATION, e -> !e.isRemoved()).isEmpty();
    }

    /**
     * Forces a wormhole to spawn on the player.
     */
    public static boolean forceSpawnToPlayer(ServerLevel level, ServerPlayer player) {
        // The command used to bypass every check, which let an op drop a second
        // player into an Ultra-Space that was already taken.
        if (UltraSpaceSession.isOccupied(level.getServer())) {
            return false;
        }

        ModEvents.cleanUp(level.getServer());
        spawnWormhole(level, player.blockPosition());
        return true;
    }

    /**
     * Attempts a random wormhole spawn.
     */
    public static void tryRandomSpawn(ServerLevel level, Random random) {
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        if (level.players().isEmpty()) return;
        if (hasActiveWormhole(level)) return;

        // A claimed dimension holds back new wormholes, even if the occupant is
        // currently offline: that is what protects their session.
        if (UltraSpaceSession.isOccupied(level.getServer())) return;

        if (random.nextInt(ConfigManager.getWormholeSpawnChance()) != 0) return;

        BlockPos spawnPos = findValidSpawnLocation(level, random);
        if (spawnPos != null) {
            spawnWormhole(level, spawnPos);
        }
    }

    /**
     * Picks a spot near a random player.
     *
     * The previous version read Heightmap.Types.WORLD_SURFACE, which counts
     * every block including leaves and snow layers, then added a fixed offset.
     * Over a spruce forest that put the portal six blocks above the TREETOPS,
     * fifteen or more blocks off the ground. MOTION_BLOCKING_NO_LEAVES gives
     * the walkable ground instead.
     *
     * It also never compared the candidate to the player's own altitude, so on
     * broken terrain the portal could appear on a neighbouring cliff top.
     */
    private static BlockPos findValidSpawnLocation(ServerLevel level, Random random) {
        var players = level.players();
        if (players.isEmpty()) return null;

        BlockPos playerPos = players.get(random.nextInt(players.size())).blockPosition();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int i = 0; i < SPAWN_ATTEMPTS; i++) {
            // Polar sampling: an even spread through the ring, instead of the
            // corner bias a random x/z offset produces.
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = MIN_DISTANCE + random.nextDouble() * (SEARCH_RADIUS - MIN_DISTANCE);

            int x = playerPos.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = playerPos.getZ() + (int) Math.round(Math.sin(angle) * distance);

            cursor.set(x, playerPos.getY(), z);

            // Never force a chunk to load just to look for a spawn.
            if (!level.hasChunkAt(cursor)) continue;

            // First free position above solid, non-leaf ground.
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

    /**
     * Enough clear room for the portal, with something solid and dry under it.
     */
    private static boolean isValidSpawnLocation(ServerLevel level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        // The portal renders two blocks tall, centred on the entity, so check
        // one below as well as the headroom above.
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

    private static void spawnWormhole(ServerLevel level, BlockPos pos) {
        level.getEntities(ModEntities.WORMHOLE, e -> true).forEach(Entity::discard);

        WormholeSpawnAnimation animation = new WormholeSpawnAnimation(
                ModEntities.WORMHOLE_ANIMATION, level, pos);
        animation.setPos(pos.getX() + 0.5, pos.getY() + 20, pos.getZ() + 0.5);
        level.addFreshEntity(animation);

        level.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0f, 0.5f);

        if (level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 64.0, false)
                instanceof ServerPlayer nearest) {
            nearest.displayClientMessage(Component.translatable("message.ultrabeasts.portal_spawn"), false);
        }
    }

    /**
     * Emits the swirl of particles converging on the portal.
     *
     * Each particle is sent with count 0 so its "velocity" fields can carry the
     * destination coordinates, which means one packet per particle. That is why
     * the count is kept modest: at 50 per tick this alone was 50 packets per
     * tick per wormhole, to every player in range.
     */

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

        if (ModParticles.WORMHOLE == null) {
            particlesUnavailable = true;
            UltraBeasts.LOGGER.warn(
                    "Particle type is not registered, disabling portal particles for this entity.");
            return;
        }

        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ() + 1;

        for (int i = 0; i < PARTICLES_PER_TICK; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;

            level.sendParticles(
                    ModParticles.WORMHOLE,
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

        enterUltraSpace(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            enterUltraSpace(serverPlayer);
        }
        super.playerTouch(player);
    }

    /** Shared by interact() and playerTouch(), which did the same thing twice. */
    private void enterUltraSpace(ServerPlayer player) {
        if (this.isRemoved()) return;

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

    private void teleportToUltraSpaceDimension(ServerPlayer player) {
        ServerLevel ultraSpace = player.getServer().getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace == null) {
            UltraBeasts.LOGGER.error("Ultra-Space dimension is not loaded, aborting teleport.");
            return;
        }

        if (player.level().dimension().equals(Level.OVERWORLD)) {
            ReturnPointStore.get(player.getServer()).put(player.getUUID(), player.blockPosition());
        }

        UltraSpaceSession.claim(player);

        String structureKey = UltraSpaceStructureManager.getRandomStructureKey(player.getRandom());
        UltraSpaceStructureManager.StructureConfig config = UltraSpaceStructureManager.getConfig(structureKey);

        if (config == null) {
            UltraBeasts.LOGGER.error("Could not find the config for {}", structureKey);
            return;
        }

        UltraSpaceStructureManager.killAllPokemonOfWorld(ultraSpace);
        grantUltraBeastsAdvancement(player);
        UltraSpaceStructureManager.placeStructure(ultraSpace, structureKey);

        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 254, false, false, true));

        BlockPos playerSpawn = config.playerSpawn();
        player.teleportTo(ultraSpace,
                playerSpawn.getX() + 0.5, playerSpawn.getY(), playerSpawn.getZ() + 0.5,
                player.getYRot(), player.getXRot());

        // The exit portal is spawned by placeStructure(), which also records its
        // position so it can be restored after a restart.

        // Was: new Thread(() -> { Thread.sleep(500); ... }).start()
        ServerScheduler.schedule(ARRIVAL_SOUND_DELAY, () -> {
            if (!player.isAlive() || player.isRemoved()) return;
            if (!player.level().dimension().equals(ModDimensions.ULTRA_SPACE_DIMENSION)) return;

            BlockPos pos = player.blockPosition();
            ultraSpace.playSound(null, pos, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 2.0f, 1.0f);
            ultraSpace.playSound(null, pos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.5f, 0.8f);

            player.displayClientMessage(Component.translatable("dimension.travel.ultra_space"), false);
        });
    }

    private void grantUltraBeastsAdvancement(ServerPlayer player) {
        ResourceLocation advancementId =
                ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "enter_ultra_space_dimension");

        AdvancementHolder advancement = player.getServer().getAdvancements().get(advancementId);
        if (advancement == null) return;

        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) return;

        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.placementTime = nbt.getLong("PlacementTime");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putLong("PlacementTime", this.placementTime);
    }
}
