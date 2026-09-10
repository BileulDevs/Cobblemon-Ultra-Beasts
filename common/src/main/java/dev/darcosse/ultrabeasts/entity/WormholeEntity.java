package dev.darcosse.ultrabeasts.entity;

import dev.darcosse.ultrabeasts.UltraBeasts;
import dev.darcosse.ultrabeasts.config.ConfigManager;
import dev.darcosse.ultrabeasts.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.registry.ModDimensions;
import dev.darcosse.ultrabeasts.registry.ModEntities;
import dev.darcosse.ultrabeasts.registry.ModEvents;
import dev.darcosse.ultrabeasts.registry.ModParticles;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class WormholeEntity extends Entity {

    private static WormholeEntity activeWormhole = null;
    private static long wormholePlacementTime = 0;

    private static final int LIFESPAN_TICKS = 1200;

    public static final Map<UUID, BlockPos> savedPositions = new HashMap<>();

    private static boolean isSpawning = false;

    private BlockPos lightBlockPos = null;

    public WormholeEntity(EntityType<? extends WormholeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    private static final int AMBIENT_SOUND_LENGTH_TICKS = 26 * 20;
    private int ambientSoundTimer = 0;

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            ServerLevel level = (ServerLevel) this.level();
            long currentTime = level.getGameTime();
            long elapsed = currentTime - wormholePlacementTime;

            if (elapsed >= LIFESPAN_TICKS) {
                this.discard();
                clearActiveWormhole();
                return;
            }

            summonPortal();
        }
    }

    /**
     * Forces a wormhole to spawn on the player.
     */
    public static void forceSpawnToPlayer(ServerLevel level, ServerPlayer player) {
        ModEvents.cleanUp(level.getServer());

        BlockPos spawnPos = player.blockPosition();
        if (spawnPos != null) {
            spawnWormhole(level, spawnPos);
        }
    }

    /**
     * Attempts a random wormhole spawn.
     */
    public static void tryRandomSpawn(ServerLevel level, Random random) {
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        if (hasActiveWormhole(level)) {
            return;
        }

        if (random.nextInt(ConfigManager.getWormholeSpawnChance()) != 0) {
            return;
        }

        BlockPos spawnPos = findValidSpawnLocation(level, random);
        if (spawnPos != null) {
            spawnWormhole(level, spawnPos);
        }
    }

    /**
     * Checks whether an active wormhole exists in the world.
     */
    public static boolean hasActiveWormhole(ServerLevel level) {
        if (isSpawning) return true;

        if (activeWormhole != null && !activeWormhole.isRemoved()) return true;

        var entities = level.getEntities(ModEntities.WORMHOLE, entity -> !entity.isRemoved());
        if (!entities.isEmpty()) {
            activeWormhole = entities.getFirst();
            return true;
        }

        return false;
    }

    /**
     * Finds a valid spawn position for the wormhole.
     */
    private static BlockPos findValidSpawnLocation(ServerLevel level, Random random) {
        if (level.players().isEmpty()) {
            return null;
        }

        var players = level.players();
        var randomPlayer = players.get(random.nextInt(players.size()));
        BlockPos playerPos = randomPlayer.blockPosition();

        int searchRadius = 20;
        int attempts = 30;
        int minHeightAboveGround = 6;

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

    /**
     * Checks whether a position is valid for an in-air spawn.
     */
    private static boolean isValidAirSpawnLocation(ServerLevel level, BlockPos pos, int airBlocksBelow) {
        for (int i = 1; i <= airBlocksBelow; i++) {
            BlockPos checkPos = pos.below(i);
            if (!level.getBlockState(checkPos).isAir()) {
                return false;
            }
        }

        for (int i = 0; i < 3; i++) {
            BlockPos checkPos = pos.above(i);
            if (!level.getBlockState(checkPos).isAir()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Spawns the wormhole at the given position.
     */
    private static void spawnWormhole(ServerLevel level, BlockPos pos) {
        level.getEntities(ModEntities.WORMHOLE, e -> true).forEach(Entity::discard);

        isSpawning = true;

        WormholeSpawnAnimation animationEntity = new WormholeSpawnAnimation(
                ModEntities.WORMHOLE_ANIMATION,
                level,
                pos
        );
        animationEntity.setPos(pos.getX() + 0.5, pos.getY() + 20, pos.getZ() + 0.5);
        level.addFreshEntity(animationEntity);

        level.playSound(
                null,
                pos,
                SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.HOSTILE,
                2.0f,
                0.5f
        );

        ServerPlayer nearestPlayer = (ServerPlayer) level.getNearestPlayer(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                64.0,
                false
        );

        if (nearestPlayer != null) {
            nearestPlayer.displayClientMessage(
                    Component.translatable("message.ultrabeasts.portal_spawn"),
                    false
            );
        }
    }

    /**
     * Resets the active wormhole state.
     */
    private static void clearActiveWormhole() {
        activeWormhole = null;
        wormholePlacementTime = 0;
        isSpawning = false;
    }

    private void summonPortal() {
        if (!(this.level() instanceof ServerLevel level)) return;

        int numberOfParticles = 50;

        double radius = 2;

        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ() + 1;

        for (int i = 0; i < numberOfParticles; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;

            double startX = centerX + Math.cos(angle) * radius;
            double startY = centerY + Math.sin(angle) * radius;
            double startZ = centerZ;

            level.sendParticles(
                    ModParticles.WORMHOLE,
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
            teleportToUltraSpaceDimension(serverPlayer);

            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);

            this.discard();
            clearActiveWormhole();

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            this.level().playSound(
                    null,
                    this.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.HOSTILE,
                    1.0f,
                    1.0f
            );

            teleportToUltraSpaceDimension(serverPlayer);

            this.discard();
            clearActiveWormhole();
        }
        super.playerTouch(player);
    }

    private void teleportToUltraSpaceDimension(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return;
        }

        ServerLevel ultraSpace = player.getServer().getLevel(ModDimensions.ULTRA_SPACE_DIMENSION);

        if (ultraSpace != null) {
            if (player.level().dimension().equals(Level.OVERWORLD)) {
                savedPositions.put(player.getUUID(), player.blockPosition());
            }

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

            BlockPos pSpawn = config.playerSpawn();
            player.teleportTo(ultraSpace, pSpawn.getX() + 0.5, pSpawn.getY(), pSpawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());

            ultraSpace.getEntities(ModEntities.RETURN_WORMHOLE, e -> true)
                    .forEach(Entity::discard);

            player.getServer().execute(() -> {
                ultraSpace.getEntities(ModEntities.RETURN_WORMHOLE, e -> true)
                        .forEach(Entity::discard);

                BlockPos rSpawn = config.returnPortalSpawn();
                ReturnWormholeEntity returnPortal = new ReturnWormholeEntity(ModEntities.RETURN_WORMHOLE, ultraSpace);
                returnPortal.setPos(
                        rSpawn.getX() + 0.5,
                        rSpawn.getY() + 2.0,
                        rSpawn.getZ() + 0.5
                );
                ultraSpace.addFreshEntity(returnPortal);
            });

            MinecraftServer server = player.getServer();
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    server.execute(() -> {
                        if (player.isAlive() && !player.isRemoved()) {
                            BlockPos playerPos = player.blockPosition();

                            ultraSpace.playSound(null, playerPos, SoundEvents.ENDERMAN_SCREAM, SoundSource.HOSTILE, 2.0f, 1.0f);
                            ultraSpace.playSound(null, playerPos, SoundEvents.WARDEN_HEARTBEAT, SoundSource.AMBIENT, 1.5f, 0.8f);

                            player.displayClientMessage(Component.translatable("dimension.travel.ultra_space"), false);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Sets the active wormhole (used by the spawn animation).
     */
    public static void setActiveWormhole(WormholeEntity wormhole, long placementTime) {
        activeWormhole = wormhole;
        wormholePlacementTime = placementTime;
        isSpawning = false;
    }

    /**
     * Marks that a spawn animation is running.
     */
    public static void setSpawning(boolean spawning) {
        isSpawning = spawning;
    }

    /**
     * Helper for the clear command.
     */
    public static void clearWormhole() {
        activeWormhole = null;
        isSpawning = false;
    }

    private void grantUltraBeastsAdvancement(ServerPlayer player) {
        ResourceLocation advancementId =
                ResourceLocation.fromNamespaceAndPath(UltraBeasts.MOD_ID, "enter_ultra_space_dimension");

        AdvancementHolder advancement = player.getServer().getAdvancements().get(advancementId);

        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);

            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(advancement, criterion);
                }
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        if (nbt.contains("PlacementTime")) {
            wormholePlacementTime = nbt.getLong("PlacementTime");
        }
        if (nbt.contains("LightBlockX") && nbt.contains("LightBlockY") && nbt.contains("LightBlockZ")) {
            lightBlockPos = new BlockPos(
                    nbt.getInt("LightBlockX"),
                    nbt.getInt("LightBlockY"),
                    nbt.getInt("LightBlockZ")
            );
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putLong("PlacementTime", wormholePlacementTime);
        if (lightBlockPos != null) {
            nbt.putInt("LightBlockX", lightBlockPos.getX());
            nbt.putInt("LightBlockY", lightBlockPos.getY());
            nbt.putInt("LightBlockZ", lightBlockPos.getZ());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this == activeWormhole) {
            activeWormhole = null;
            wormholePlacementTime = 0;
        }
    }
}
