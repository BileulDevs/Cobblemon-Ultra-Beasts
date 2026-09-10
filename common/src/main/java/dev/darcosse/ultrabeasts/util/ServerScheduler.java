package dev.darcosse.ultrabeasts.util;

import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Runs a task after a delay, on the server thread.
 *
 * Replaces the raw {@code new Thread(() -> Thread.sleep(...))} pattern: that
 * spawned one OS thread per teleport, captured entities across threads, and
 * kept running even if the server shut down in between.
 *
 * Drained once per tick by {@link dev.darcosse.ultrabeasts.registry.ModEvents}.
 */
public final class ServerScheduler {

    private record Task(long dueTick, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static long currentTick = 0;

    private ServerScheduler() {
    }

    /**
     * @param delayTicks how many ticks to wait (20 ticks = 1 second)
     */
    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new Task(currentTick + Math.max(0, delayTicks), action));
    }

    public static void tick(MinecraftServer server) {
        currentTick++;

        if (TASKS.isEmpty()) return;

        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (task.dueTick() <= currentTick) {
                it.remove();
                task.action().run();
            }
        }
    }

    /** Called on server stop so pending tasks never leak into the next session. */
    public static void clear() {
        TASKS.clear();
        currentTick = 0;
    }
}
