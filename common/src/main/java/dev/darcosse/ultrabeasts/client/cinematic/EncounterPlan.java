package dev.darcosse.ultrabeasts.client.cinematic;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The geometry of the encounter cinematic's shots, without any game state: the fly-over
 * around an open structure, the walkthrough of an enclosed one, the dolly toward the Ultra
 * Beast and the final zoom on its head. Kept apart from the screen so the camera paths can
 * be read, and tuned, on their own. Collision is checked by the screen, which passes the
 * results in.
 * <p>
 * Every progress argument runs from 0 (start of the shot) to 1 (end).
 */
public final class EncounterPlan {

    // ---- fly-over ---------------------------------------------------------

    /**
     * Orbit radius as a share of the structure's half span. Below 1 the camera stays over
     * the structure rather than around it, which keeps the far edge within render distance
     * on the long layouts (celesteela is 192 blocks deep).
     */
    private static final double ORBIT_RADIUS_SHARE = 0.8;

    /** Orbit radius bounds, in blocks. The upper one keeps the structure filling the frame. */
    private static final double ORBIT_MIN_RADIUS = 24.0;
    private static final double ORBIT_MAX_RADIUS = 64.0;

    /**
     * Height of the camera above the focus at the start of the fly-over, in blocks, plus a
     * share of the radius so a wider orbit also looks down more. Lower brings the camera
     * into the structure's spires.
     */
    private static final double ORBIT_BASE_HEIGHT = 18.0;
    private static final double ORBIT_HEIGHT_PER_RADIUS = 0.25;

    /** How far the camera sinks during the fly-over, in blocks: gives the shot some depth. */
    private static final double ORBIT_DESCENT = 6.0;

    /** Angle swept by the fly-over, in degrees. Larger turns faster for the same duration. */
    private static final double ORBIT_SWEEP_DEGREES = 120.0;

    // ---- walkthrough ------------------------------------------------------

    /** Camera height above the arrival point's feet, in blocks: a little above a player's eyes. */
    public static final double WALK_EYE_HEIGHT = 2.2;

    /** How far behind the arrival point the walkthrough starts, in blocks. */
    public static final double WALK_BACK = 1.5;

    /**
     * Share of the way to the beast covered by the walkthrough. The close-up takes over from
     * there; higher leaves the cut with less ground to cover, lower shows less of the cave.
     */
    public static final double WALK_SHARE = 0.6;

    /**
     * Largest look-around angle of the walkthrough, in degrees. The camera pans left, then
     * right, then settles on the beast; wider shows more of the walls but reads as nervous.
     */
    private static final double WALK_SWAY_DEGREES = 28.0;

    // ---- close-up ---------------------------------------------------------

    /**
     * Final camera distance, as a multiple of the beast's largest dimension, plus a fixed
     * margin in blocks. The margin keeps small beasts (poipole, under a block) from filling
     * the whole screen.
     */
    private static final double CLOSE_UP_SIZE_FACTOR = 1.6;
    private static final double CLOSE_UP_MARGIN = 2.5;

    /** Starting distance, as a multiple of the final one: how far the dolly travels. */
    private static final double CLOSE_UP_DOLLY_FACTOR = 1.6;

    /** Camera height above the aim point, as a share of the beast's size: a slight downward look. */
    private static final double CLOSE_UP_RISE_SHARE = 0.25;

    /** Share of the beast's height aimed at: a little above its middle. */
    private static final double AIM_HEIGHT_SHARE = 0.55;

    /** Size assumed while the beast has not reached the client, in blocks. */
    public static final double FALLBACK_BEAST_SIZE = 2.0;

    // ---- head zoom --------------------------------------------------------

    /**
     * Camera distance from the head during the cry, as a multiple of the beast's width,
     * plus a margin in blocks. Width rather than height: a tall, thin beast (xurkitree) would
     * otherwise be filmed from too far to fill the frame.
     */
    private static final double HEAD_WIDTH_FACTOR = 0.8;
    private static final double HEAD_MARGIN = 1.6;

    /** Camera height above the head, as a share of the distance: almost eye to eye. */
    private static final double HEAD_RISE_SHARE = 0.1;

    /**
     * Largest shake offset at the start of the cry, in blocks. It decays to nothing; above
     * ~0.15 the image blurs instead of shaking.
     */
    private static final double SHAKE_AMPLITUDE = 0.08;

    /** Shake frequencies, in radians per second: unrelated, so the motion does not loop. */
    private static final double SHAKE_FREQ_X = 47.0;
    private static final double SHAKE_FREQ_Y = 61.0;
    private static final double SHAKE_FREQ_Z = 53.0;

    private final Vec3 focus;
    private final double radius;
    private final double startAngle;
    private final double baseHeight;

    /**
     * @param structureMin lowest corner of the structure
     * @param structureMax highest corner of the structure
     * @param beastPos     where the Ultra Beast stands
     * @param viewerPos    where the player arrived; the fly-over starts on their side
     */
    public EncounterPlan(BlockPos structureMin, BlockPos structureMax, Vec3 beastPos, Vec3 viewerPos) {
        // The box top is the template height (48 or 96), often far above anything built,
        // so the focus height comes from the two points of interest instead.
        double focusY = (beastPos.y + viewerPos.y) / 2.0;
        this.focus = new Vec3(
                (structureMin.getX() + structureMax.getX() + 1) / 2.0,
                focusY,
                (structureMin.getZ() + structureMax.getZ() + 1) / 2.0);

        double halfSpan = Math.max(
                structureMax.getX() - structureMin.getX() + 1,
                structureMax.getZ() - structureMin.getZ() + 1) / 2.0;
        this.radius = Mth.clamp(halfSpan * ORBIT_RADIUS_SHARE, ORBIT_MIN_RADIUS, ORBIT_MAX_RADIUS);
        this.baseHeight = focusY + ORBIT_BASE_HEIGHT + radius * ORBIT_HEIGHT_PER_RADIUS;
        this.startAngle = Math.atan2(viewerPos.z - focus.z, viewerPos.x - focus.x);
    }

    /** The fly-over at a given progress: circling the structure while looking at its focus. */
    public EncounterCamera.Shot orbit(double progress) {
        double eased = easeInOut(progress);
        double angle = startAngle + Math.toRadians(ORBIT_SWEEP_DEGREES) * eased;
        Vec3 position = new Vec3(
                focus.x + Math.cos(angle) * radius,
                baseHeight - ORBIT_DESCENT * eased,
                focus.z + Math.sin(angle) * radius);
        return new EncounterCamera.Shot(position, focus);
    }

    /**
     * The walkthrough at a given progress: gliding from {@code start} to {@code end} while
     * looking around, then settling on the beast.
     *
     * @param start where the glide begins, already checked for walls by the caller
     * @param end   where it ends, likewise
     * @param aim   the beast's aim point, looked at once the sway has died out
     */
    public static EncounterCamera.Shot walkthrough(double progress, Vec3 start, Vec3 end, Vec3 aim) {
        double eased = easeInOut(progress);
        Vec3 position = start.lerp(end, eased);

        // One full left-right sway that fades out, so the shot ends looking at the beast.
        double clamped = Mth.clamp(progress, 0.0, 1.0);
        double sway = Math.toRadians(WALK_SWAY_DEGREES) * Math.sin(clamped * Math.PI * 2.0) * (1.0 - clamped);
        Vec3 toward = aim.subtract(position);
        double cos = Math.cos(sway);
        double sin = Math.sin(sway);
        Vec3 turned = new Vec3(toward.x * cos - toward.z * sin, toward.y, toward.x * sin + toward.z * cos);
        return new EncounterCamera.Shot(position, position.add(turned));
    }

    /** Where the close-up aims for a beast of the given height standing at {@code feet}. */
    public static Vec3 aimPoint(Vec3 feet, double beastHeight) {
        return feet.add(0.0, beastHeight * AIM_HEIGHT_SHARE, 0.0);
    }

    /**
     * The unobstructed camera position at the start of the dolly, before collision is
     * checked: on the viewer's side of the beast, so the camera stands in the arena rather
     * than behind the beast, where structures often put a wall.
     */
    public static Vec3 closeUpStart(Vec3 aim, Vec3 viewerPos, double beastSize) {
        return aim.add(towardViewer(aim, viewerPos, startDistance(beastSize), beastSize * CLOSE_UP_RISE_SHARE));
    }

    /**
     * The close-up at a given progress: dollying from the start distance to the final one.
     *
     * @param maxFraction share of the way from the aim point to {@link #closeUpStart} that is
     *                    free of blocks; the camera never goes past it, so a wall on the
     *                    viewer's side shortens the shot instead of being filmed from inside
     */
    public static EncounterCamera.Shot closeUp(double progress, Vec3 aim, Vec3 viewerPos, double beastSize,
                                                double maxFraction) {
        double eased = easeOut(progress);
        double full = startDistance(beastSize);
        double distance = Mth.lerp(eased, full, finalDistance(beastSize));
        // Scaling the whole offset, rise included, keeps the camera on the checked ray.
        double fraction = Math.min(distance / full, maxFraction);
        Vec3 offset = towardViewer(aim, viewerPos, full, beastSize * CLOSE_UP_RISE_SHARE).scale(fraction);
        return new EncounterCamera.Shot(aim.add(offset), aim);
    }

    /** The unobstructed head shot position, before collision is checked. */
    public static Vec3 headStart(Vec3 head, Vec3 viewerPos, double beastWidth) {
        double distance = headDistance(beastWidth);
        return head.add(towardViewer(head, viewerPos, distance, distance * HEAD_RISE_SHARE));
    }

    /**
     * The zoom on the head during the cry: blends from the end of the close-up to a tight
     * shot of the head, with a shake that dies out.
     *
     * @param zoom        progress of the blend toward the head shot, 0 to 1
     * @param shake       progress of the shake, 0 (strongest) to 1 (gone)
     * @param seconds     time since the cry, for the shake's oscillation
     * @param from        the close-up's last shot, where the zoom starts
     * @param head        the beast's head (its eyes)
     * @param maxFraction free share of the way from the head to {@link #headStart}
     */
    public static EncounterCamera.Shot headZoom(double zoom, double shake, double seconds, EncounterCamera.Shot from,
                                                 Vec3 head, Vec3 viewerPos, double beastWidth, double maxFraction) {
        Vec3 target = head.add(headStart(head, viewerPos, beastWidth).subtract(head).scale(Math.min(1.0, maxFraction)));
        double eased = easeInOut(zoom);
        Vec3 position = from.position().lerp(target, eased);
        Vec3 look = from.look().lerp(head, eased);

        double amplitude = SHAKE_AMPLITUDE * (1.0 - Mth.clamp(shake, 0.0, 1.0));
        if (amplitude > 0.0) {
            position = position.add(
                    Math.sin(seconds * SHAKE_FREQ_X) * amplitude,
                    Math.sin(seconds * SHAKE_FREQ_Y) * amplitude * 0.6,
                    Math.cos(seconds * SHAKE_FREQ_Z) * amplitude);
        }
        return new EncounterCamera.Shot(position, look);
    }

    /** Distance at the start of the dolly. */
    private static double startDistance(double beastSize) {
        return finalDistance(beastSize) * CLOSE_UP_DOLLY_FACTOR;
    }

    /** Distance at the end of the dolly. */
    private static double finalDistance(double beastSize) {
        return beastSize * CLOSE_UP_SIZE_FACTOR + CLOSE_UP_MARGIN;
    }

    /** Distance of the head shot. */
    private static double headDistance(double beastWidth) {
        return beastWidth * HEAD_WIDTH_FACTOR + HEAD_MARGIN;
    }

    /**
     * Offset from {@code from} toward the viewer, horizontally, at a distance, raised by
     * {@code rise}. Taken toward the viewer because that side is the arena they walk in.
     */
    private static Vec3 towardViewer(Vec3 from, Vec3 viewerPos, double distance, double rise) {
        Vec3 dir = horizontalDirection(from, viewerPos);
        return new Vec3(dir.x * distance, rise, dir.z * distance);
    }

    /**
     * Unit horizontal vector from {@code from} to {@code to}. When the two share X and Z
     * (never by layout, but a command could do it), any direction is as good as another.
     */
    public static Vec3 horizontalDirection(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-3) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return new Vec3(dx / length, 0.0, dz / length);
    }

    /** Smooth start and stop: the camera neither jerks into motion nor stops dead. */
    private static double easeInOut(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    /** Fast start, slow arrival: the dolly settles on the beast. */
    private static double easeOut(double t) {
        double c = 1.0 - Mth.clamp(t, 0.0, 1.0);
        return 1.0 - c * c * c;
    }
}
