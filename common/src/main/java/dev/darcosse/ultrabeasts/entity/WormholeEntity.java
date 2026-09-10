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

    private static BlockPos findValidSpawnLocation(ServerLevel level, Random random) {
        var players = level.players();
        if (players.isEmpty()) return null;

        BlockPos playerPos = players.get(random.nextInt(players.size())).blockPosition();

        final int searchRadius = 20;
        final int attempts = 30;
        final int minHeightAboveGround = 6;

        for (int i = 0; i < attempts; i++) {
            int x = playerPos.getX() + random.nextInt(searchRadius * 2) - searchRadius;
            int z = playerPos.getZ() + random.nextInt(searchRadius * 2) - searchRadius;

            BlockPos surfacePos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE,
                    new BlockPos(x, level.getMaxBuildHeight(), z));
            BlockPos spawnPos = surfacePos.above(minHeightAboveGround);

            if (isValidAirSpawnLocation(level, spawnPos, minHeightAboveGround)) {
                return spawnPos;
            }
        }

        return null;
    }

    private static boolean isValidAirSpawnLocation(ServerLevel level, BlockPos pos, int airBlocksBelow) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int i = 1; i <= airBlocksBelow; i++) {
            cursor.set(pos.getX(), pos.getY() - i, pos.getZ());
            if (!level.getBlockState(cursor).isAir()) return false;
        }

        for (int i = 0; i < 3; i++) {
            cursor.set(pos.getX(), pos.getY() + i, pos.getZ());
            if (!level.getBlockState(cursor).isAir()) return false;
        }

        return true;
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
    private void summonPortal(ServerLevel level) {
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
