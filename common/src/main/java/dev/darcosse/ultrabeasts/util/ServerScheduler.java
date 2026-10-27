package dev.darcosse.ultrabeasts.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs tasks after a delay, on the server thread.
 * <p>
 * Replaces the old {@code new Thread(() -> Thread.sleep(...))} pattern, which spawned one OS
 * thread per teleport, touched entities from outside the server thread, and kept running
 * even if the server shut down in between.
 * <p>
 * Drained once per server tick by {@link dev.darcosse.ultrabeasts.registry.ModEvents}.
 * Server side only, not thread-safe: schedule from the server thread.
 */
public final class ServerScheduler {

    /** A pending action and the scheduler tick at which it becomes due. */
    private record Task(long dueTick, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();

    /**
     * Ticks counted by this scheduler itself rather than read from a level: it keeps working
     * even if the overworld's game time is changed by {@code /time}.
     */
    private static long currentTick = 0;

    /** Static utility, never instantiated. */
    private ServerScheduler() {
    }

    /**
     * Queues an action to run on a later server tick.
     *
     * @param delayTicks how many ticks to wait (20 ticks = 1 second); negative values run on
     *                   the next tick
     * @param action     the work to run; it may itself schedule further tasks
     */
    public static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new Task(currentTick + Math.max(0, delayTicks), action));
    }

    /** Advances the clock by one tick and runs every task that has become due. */
    public static void tick() {
        currentTick++;

        if (TASKS.isEmpty()) return;

        // Split first, run afterwards: running inside the iteration would throw a
        // ConcurrentModificationException as soon as a task schedules another one.
        List<Task> due = null;
        for (int i = TASKS.size() - 1; i >= 0; i--) {
            Task task = TASKS.get(i);
            if (task.dueTick() <= currentTick) {
                if (due == null) due = new ArrayList<>();
                due.add(task);
                TASKS.remove(i);
            }
        }

        if (due == null) return;

        // Collected back to front; run in scheduling order.
        for (int i = due.size() - 1; i >= 0; i--) {
            due.get(i).action().run();
        }
    }

    /** Drops every pending task. Called on server stop so nothing leaks into the next world. */
    public static void clear() {
        TASKS.clear();
        currentTick = 0;
    }
}
