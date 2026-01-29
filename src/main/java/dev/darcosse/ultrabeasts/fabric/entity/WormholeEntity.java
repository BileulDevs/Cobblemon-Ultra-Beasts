package dev.darcosse.ultrabeasts.fabric.entity;

import com.cobblemon.mod.common.CobblemonEntities;
import dev.darcosse.ultrabeasts.fabric.UltraBeasts;
import dev.darcosse.ultrabeasts.fabric.config.ConfigManager;
import dev.darcosse.ultrabeasts.fabric.dimension.UltraSpaceStructureManager;
import dev.darcosse.ultrabeasts.fabric.registry.*;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

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

    public WormholeEntity(EntityType<? extends WormholeEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setInvulnerable(true);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    private static final int AMBIENT_SOUND_LENGTH_TICKS = 26 * 20;
    private int ambientSoundTimer = 0;

    @Override
    public void tick() {
        super.tick();

        if (!this.getWorld().isClient) {
            ServerWorld world = (ServerWorld) this.getWorld();
            long currentTime = world.getTime();
            long elapsed = currentTime - wormholePlacementTime;

            if (elapsed >= LIFESPAN_TICKS) {
                this.discard();
                clearActiveWormhole();
                return;
            }

//            ambientSoundTimer++;
//            if (ambientSoundTimer >= AMBIENT_SOUND_LENGTH_TICKS || ambientSoundTimer == 1) {
//                world.playSound(
//                        null,
//                        this.getBlockPos(),
//                        ModSounds.WORMHOLE_AMBIENT,
//                        SoundCategory.HOSTILE,
//                        1.0f,
//                        0.5f
//                );
//                ambientSoundTimer = 0;
//            }

            summonPortal();
        }
    }

    /**
     * Méthode statique pour forcer le spawn d'un trou de ver au joueur
     */
    public static void forceSpawnToPlayer(ServerWorld world, ServerPlayerEntity player) {
        ModEvents.cleanUp(world.getServer());

        BlockPos spawnPos = player.getBlockPos();
        if (spawnPos != null) {
            spawnWormhole(world, spawnPos);
        }
    }

    /**
     * Méthode statique pour tenter de faire spawner un trou de ver aléatoirement
     */
    public static void tryRandomSpawn(ServerWorld world, Random random) {
        if (!world.getRegistryKey().equals(World.OVERWORLD)) {
            return;
        }

        if (hasActiveWormhole(world)) {
            return;
        }

        if (random.nextInt(ConfigManager.getWormholeSpawnChance()) != 0) {
            return;
        }

        BlockPos spawnPos = findValidSpawnLocation(world, random);
        if (spawnPos != null) {
            spawnWormhole(world, spawnPos);
        }
    }

    /**
     * Vérifie s'il y a un trou de ver actif dans le monde
     */
    public static boolean hasActiveWormhole(ServerWorld world) {
        if (isSpawning) return true;

        if (activeWormhole != null && !activeWormhole.isRemoved()) return true;

        var entities = world.getEntitiesByType(ModEntities.WORMHOLE, entity -> !entity.isRemoved());
        if (!entities.isEmpty()) {
            activeWormhole = entities.getFirst();
            return true;
        }

        return false;
    }

    /**
     * Trouve une position valide pour faire spawner le trou de ver
     */
    private static BlockPos findValidSpawnLocation(ServerWorld world, Random random) {
        if (world.getPlayers().isEmpty()) {
            return null;
        }

        var players = world.getPlayers();
        var randomPlayer = players.get(random.nextInt(players.size()));
        BlockPos playerPos = randomPlayer.getBlockPos();

        int searchRadius = 20;
        int attempts = 30;
        int minHeightAboveGround = 6;

        for (int i = 0; i < attempts; i++) {
            int x = playerPos.getX() + random.nextInt(searchRadius * 2) - searchRadius;
            int z = playerPos.getZ() + random.nextInt(searchRadius * 2) - searchRadius;

            BlockPos surfacePos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, new BlockPos(x, world.getTopY(), z));
            BlockPos spawnPos = surfacePos.up(minHeightAboveGround);

            if (isValidAirSpawnLocation(world, spawnPos, minHeightAboveGround)) {
                return spawnPos;
            }
        }

        return null;
    }

    /**
     * Vérifie si une position est valide pour le spawn en l'air
     */
    private static boolean isValidAirSpawnLocation(ServerWorld world, BlockPos pos, int airBlocksBelow) {
        for (int i = 1; i <= airBlocksBelow; i++) {
            BlockPos checkPos = pos.down(i);
            if (!world.getBlockState(checkPos).isAir()) {
                return false;
            }
        }

        for (int i = 0; i < 3; i++) {
            BlockPos checkPos = pos.up(i);
            if (!world.getBlockState(checkPos).isAir()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Fait spawner le trou de ver à la position donnée
     */
    private static void spawnWormhole(ServerWorld world, BlockPos pos) {
        world.getEntitiesByType(ModEntities.WORMHOLE, e -> true).forEach(Entity::discard);

        isSpawning = true;

        WormholeSpawnAnimation animationEntity = new WormholeSpawnAnimation(
                ModEntities.WORMHOLE_ANIMATION,
                world,
                pos
        );
        animationEntity.setPosition(pos.getX() + 0.5, pos.getY() + 20, pos.getZ() + 0.5);
        world.spawnEntity(animationEntity);

        world.playSound(
                null,
                pos,
                SoundEvents.ENTITY_WARDEN_SONIC_BOOM,
                SoundCategory.HOSTILE,
                2.0f,
                0.5f
        );

        ServerPlayerEntity nearestPlayer = (ServerPlayerEntity) world.getClosestPlayer(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                64.0,
                false
        );

        if (nearestPlayer != null) {
            nearestPlayer.sendMessage(
                    Text.translatable("message.ultrabeasts.portal_spawn"),
                    false
            );
        }
    }

    /**
     * Nettoie les variables du trou de ver actif
     */
    private static void clearActiveWormhole() {
        activeWormhole = null;
        wormholePlacementTime = 0;
        isSpawning = false;
    }

    private void summonPortal() {
        if (!(this.getWorld() instanceof ServerWorld world)) return;

        int numberOfParticles = 50;

        double radius = 2;

        double centerX = this.getX();
        double centerY = this.getY();
        double centerZ = this.getZ() + 1;

        for (int i = 0; i < numberOfParticles; i++) {
            double angle = world.random.nextDouble() * 2 * Math.PI;

            double startX = centerX + Math.cos(angle) * radius;
            double startY = centerY + Math.sin(angle) * radius;
            double startZ = centerZ;

            world.spawnParticles(
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
    public ActionResult interact(PlayerEntity player, Hand hand) {
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            teleportToChimerasDimension(serverPlayer);

            this.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

            this.discard();
            clearActiveWormhole();

            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            this.getWorld().playSound(
                    null,
                    this.getBlockPos(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.HOSTILE,
                    1.0f,
                    1.0f
            );

            teleportToChimerasDimension(serverPlayer);

            this.discard();
            clearActiveWormhole();
        }
        super.onPlayerCollision(player);
    }

    private void teleportToChimerasDimension(ServerPlayerEntity player) {
        ServerWorld ultraSpace = player.getServer().getWorld(ModDimensions.ULTRA_SPACE_DIMENSION);
        if (ultraSpace != null) {
            if (player.getWorld().getRegistryKey().equals(World.OVERWORLD)) {
                savedPositions.put(player.getUuid(), player.getBlockPos());
            }

            killAllPokemonsOfWorld(ultraSpace);
            grantUltraBeastsAdvancement(player);

            UltraSpaceStructureManager.placeStructure(ultraSpace);

            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 254, false, false, true));

            player.teleport(ultraSpace, 0.5, 83, -19.5, player.getYaw(), player.getPitch());

            ReturnWormholeEntity returnPortal = new ReturnWormholeEntity(ModEntities.RETURN_WORMHOLE, ultraSpace);
            returnPortal.setPosition(0.5, 83, -16.5);
            ultraSpace.spawnEntity(returnPortal);

            MinecraftServer server = player.getServer();
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                    server.execute(() -> {
                        if (player.isAlive() && !player.isRemoved()) {
                            BlockPos playerPos = player.getBlockPos();

                            ultraSpace.playSound(
                                    null,
                                    playerPos,
                                    SoundEvents.ENTITY_ENDERMAN_SCREAM,
                                    SoundCategory.HOSTILE,
                                    2.0f,
                                    1.0f
                            );

                            ultraSpace.playSound(
                                    null,
                                    playerPos,
                                    SoundEvents.ENTITY_WARDEN_HEARTBEAT,
                                    SoundCategory.AMBIENT,
                                    1.5f,
                                    0.8f
                            );

                            player.sendMessage(Text.translatable("dimension.travel.ultra_space"), false);
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Définit le trou de ver actif (utilisé par l'animation)
     */
    public static void setActiveWormhole(WormholeEntity wormhole, long placementTime) {
        activeWormhole = wormhole;
        wormholePlacementTime = placementTime;
        isSpawning = false;
    }

    /**
     * Marque qu'une animation de spawn est en cours
     */
    public static void setSpawning(boolean spawning) {
        isSpawning = spawning;
    }

    /**
     * Fonction pour aide au clear commande
     */
    public static void clearWormhole() {
        activeWormhole = null;
        isSpawning = false;
    }

    public static void killAllPokemonsOfWorld(ServerWorld ultraSpace) {
        if (ultraSpace == null || ultraSpace.getPlayers().isEmpty()) {
            return;
        }

        ultraSpace.iterateEntities().forEach(entity -> {
            if (entity != null && entity.getType() == CobblemonEntities.POKEMON) {
                entity.discard();
            }
        });
    }

    private void grantUltraBeastsAdvancement(ServerPlayerEntity player) {
        Identifier advancementId = Identifier.of(UltraBeasts.MOD_ID, "enter_ultra_space_dimension");

        AdvancementEntry advancement = player.getServer().getAdvancementLoader().get(advancementId);

        if (advancement != null) {
            AdvancementProgress progress = player.getAdvancementTracker().getProgress(advancement);

            if (!progress.isDone()) {
                for (String criterion : progress.getUnobtainedCriteria()) {
                    player.getAdvancementTracker().grantCriterion(advancement, criterion);
                }
            }
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
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
    protected void writeCustomDataToNbt(NbtCompound nbt) {
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