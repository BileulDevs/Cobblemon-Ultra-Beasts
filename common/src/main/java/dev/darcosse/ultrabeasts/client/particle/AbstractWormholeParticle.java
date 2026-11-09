package dev.darcosse.ultrabeasts.client.particle;

import dev.darcosse.ultrabeasts.entity.AbstractPortalEntity;
import dev.darcosse.ultrabeasts.entity.PortalSwirl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Swirl particle that flies from the portal's rim to its centre while shifting from pink to
 * white. Shared by the inbound and outbound portals, which only differ by the Z offset used
 * when no explicit target is given. Client side only.
 * <p>
 * The velocity arguments carry the TARGET POINT, not a velocity: the portal passes the
 * point the swirl converges on (see {@code AbstractPortalEntity.spawnSwirlParticles}). The
 * axis of the funnel, which used to be world Z, comes through {@link PortalSwirl#pendingAxis()}.
 */
public abstract class AbstractWormholeParticle extends TextureSheetParticle {

    /**
     * Below this on both X and Y, the velocity is treated as "no target given". A real target
     * is a world position, so this only misfires for a portal sitting at x = y = 0.
     */
    private static final double NO_TARGET_EPSILON = 0.0001;

    /** Half-size of the box searched for a portal when no target is given, in blocks. */
    private static final double PORTAL_SEARCH_RADIUS = 5;

    /**
     * Random spread of the start point along the swirl axis (±0.25 block), so the swirl has
     * some depth.
     */
    private static final double START_AXIAL_JITTER = 0.5;

    /** Shortest lifetime, in ticks. */
    private static final int MIN_LIFETIME = 25;

    /** Random extra lifetime, in ticks: 25 to 44 in total. */
    private static final int LIFETIME_SPREAD = 20;

    /** Quad size at birth; it shrinks linearly to 10% of that at death. */
    private static final float BASE_QUAD_SIZE = 0.4f;

    /**
     * Distance to the camera, in blocks, below which a particle starts to shrink, and the one
     * at which it has vanished.
     * <p>
     * Seen from the front, the swirl's ring lies between the portal and the player, a block
     * or two from the eye: quads of 0.44 block at that distance filled half the screen with
     * pink discs. From the side or the back the ring is farther away and nothing changes.
     * Raising NEAR_FULL_SIZE thins the funnel's mouth for a player standing close; lowering
     * NEAR_VANISH below ~0.5 brings back the discs right against the eye.
     */
    private static final double NEAR_FULL_SIZE = 3.0;
    private static final double NEAR_VANISH = 1.0;

    // Colour ramp, as fractions of the lifetime: pink, then lilac, then azure, then white.
    private static final float PINK_UNTIL = 0.08f;
    private static final float LILAC_UNTIL = 0.15f;
    private static final float AZURE_UNTIL = 0.22f;

    private final double destX, destY, destZ;

    /** Unit axis of the swirl (see {@code PortalSwirl}): world Z for the exit portal. */
    private final double axisX, axisY, axisZ;

    /**
     * Start distance from the target along the axis, jitter included. Covered linearly: it is
     * what draws the funnel's depth.
     */
    private final double startAxial;

    /**
     * Start offset from the target across the axis. Covered with a cubic ease-out: the
     * particle rushes in, then slows near the centre.
     */
    private final double radialX, radialY, radialZ;

    /**
     * @param vX       target X (see the class comment), or ~0 to search for a nearby portal
     * @param vY       target Y
     * @param vZ       target Z
     * @param zOffset  added to the found portal's Z when no explicit target is given
     */
    protected AbstractWormholeParticle(ClientLevel level, double x, double y, double z,
                                       double vX, double vY, double vZ, SpriteSet sprite,
                                       double zOffset) {
        super(level, x, y, z, 0, 0, 0);

        boolean hasTarget = Math.abs(vX) > NO_TARGET_EPSILON || Math.abs(vY) > NO_TARGET_EPSILON;
        if (hasTarget) {
            this.destX = vX;
            this.destY = vY;
            this.destZ = vZ;
        } else {
            // Fallback for a particle spawned without a target (e.g. by /particle): converge
            // on the nearest portal, or in place if there is none.
            AABB searchBox = new AABB(x - PORTAL_SEARCH_RADIUS, y - PORTAL_SEARCH_RADIUS, z - PORTAL_SEARCH_RADIUS,
                    x + PORTAL_SEARCH_RADIUS, y + PORTAL_SEARCH_RADIUS, z + PORTAL_SEARCH_RADIUS);

            Entity portal = level.getEntitiesOfClass(AbstractPortalEntity.class, searchBox, e -> true)
                    .stream().findFirst().orElse(null);

            if (portal != null) {
                this.destX = portal.getX();
                this.destY = portal.getY();
                this.destZ = portal.getZ() + zOffset;
            } else {
                this.destX = x;
                this.destY = y;
                this.destZ = z + zOffset;
            }
        }

        this.rCol = 1.0f;
        this.gCol = 0.8f;
        this.bCol = 0.9f;

        // A particle without an explicit target falls back on world Z, the axis the
        // fallback offset above is expressed in.
        Vec3 axis = hasTarget ? PortalSwirl.pendingAxis() : PortalSwirl.DEFAULT_AXIS;
        this.axisX = axis.x;
        this.axisY = axis.y;
        this.axisZ = axis.z;

        double offsetX = x - destX;
        double offsetY = y - destY;
        double offsetZ = z - destZ;
        double axial = offsetX * axisX + offsetY * axisY + offsetZ * axisZ;
        this.radialX = offsetX - axisX * axial;
        this.radialY = offsetY - axisY * axial;
        this.radialZ = offsetZ - axisZ * axial;
        this.startAxial = axial + (level.random.nextDouble() * START_AXIAL_JITTER - START_AXIAL_JITTER / 2);

        this.lifetime = MIN_LIFETIME + level.random.nextInt(LIFETIME_SPREAD);
        this.pickSprite(sprite);
        this.gravity = 0.0f;
    }

    /** Moves the particle along its curve and updates its colour, alpha and size. */
    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        float progress = (float) this.age / this.lifetime;

        // Along the axis the particle moves linearly; across it, it follows a cubic ease-out.
        // With the axis on world Z this is exactly the motion of 4.1 and before.
        double axial = startAxial * (1.0 - progress);
        double curve = Math.pow(1.0 - progress, 3);
        this.x = destX + axisX * axial + radialX * curve;
        this.y = destY + axisY * axial + radialY * curve;
        this.z = destZ + axisZ * axial + radialZ * curve;

        if (progress < PINK_UNTIL) {
            this.rCol = 1.0f;
            this.gCol = 0.8f;
            this.bCol = 0.9f;
        } else if (progress < LILAC_UNTIL) {
            float local = (progress - PINK_UNTIL) / (LILAC_UNTIL - PINK_UNTIL);
            this.rCol = lerp(local, 1.0f, 0.8f);
            this.gCol = lerp(local, 0.8f, 0.7f);
            this.bCol = lerp(local, 0.9f, 1.0f);
        } else if (progress < AZURE_UNTIL) {
            float local = (progress - LILAC_UNTIL) / (AZURE_UNTIL - LILAC_UNTIL);
            this.rCol = lerp(local, 0.8f, 0.6f);
            this.gCol = lerp(local, 0.7f, 0.85f);
            this.bCol = lerp(local, 1.0f, 1.0f);
        } else {
            // Azure to white over the first half of the remaining life, then pure white.
            float local = (progress - AZURE_UNTIL) / (1.0f - AZURE_UNTIL);

            this.rCol = lerp(Math.min(local * 2.0f, 1.0f), 0.6f, 1.0f);
            this.gCol = lerp(Math.min(local * 2.0f, 1.0f), 0.85f, 1.0f);
            this.bCol = 1.0f;
        }

        // The opaque sheet draws without blending, so this fade may only show through the
        // shader's alpha cut-off. Switching to PARTICLE_SHEET_TRANSLUCENT would make it a real
        // fade, but changes the look: left as is on purpose.
        this.alpha = (float) Math.sin(Math.PI * progress);
        this.quadSize = BASE_QUAD_SIZE * (1.1f - progress);
    }

    /**
     * Shrinks the particle as it nears the camera (see {@link #NEAR_FULL_SIZE}). Called once
     * per particle per frame: one camera lookup and one distance, cheap next to the quad
     * itself.
     */
    @Override
    public float getQuadSize(float partialTick) {
        Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double px = this.xo + (this.x - this.xo) * partialTick;
        double py = this.yo + (this.y - this.yo) * partialTick;
        double pz = this.zo + (this.z - this.zo) * partialTick;
        double distance = Math.sqrt(eye.distanceToSqr(px, py, pz));
        if (distance >= NEAR_FULL_SIZE) {
            return this.quadSize;
        }
        double factor = Math.max(0.0, (distance - NEAR_VANISH) / (NEAR_FULL_SIZE - NEAR_VANISH));
        return (float) (this.quadSize * factor);
    }

    /** Linear interpolation from start to end. */
    private float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    /** Always fully lit: the swirl glows in the dark of Ultra-Space. */
    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }
}
