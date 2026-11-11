package xyz.frxzenluke.titanreborn.manager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs delayed game actions on the Minecraft server thread. */
public final class ServerScheduler {
    private static final ServerScheduler SHARED = new ServerScheduler();
    private final List<Task> tasks = new ArrayList<>();
    private long ticks;
    private boolean stopped;

    public ServerScheduler() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    public static ServerScheduler shared() {
        return SHARED;
    }

    public TaskHandle schedule(MinecraftServer server, Runnable action, long delay, TimeUnit unit) {
        return add(server, action, delay, 0, unit);
    }

    public TaskHandle scheduleAtFixedRate(MinecraftServer server, Runnable action, long delay, long period, TimeUnit unit) {
        if (period <= 0) {
            throw new IllegalArgumentException("period must be positive");
        }
        return add(server, action, delay, period, unit);
    }

    private TaskHandle add(MinecraftServer server, Runnable action, long delay, long period, TimeUnit unit) {
        if (stopped) {
            throw new IllegalStateException("scheduler is stopped");
        }
        Task task = new Task(server, action, ticks + Math.max(1, toTicks(delay, unit)), toTicks(period, unit));
        tasks.add(task);
        return () -> task.cancelled = true;
    }

    private static long toTicks(long duration, TimeUnit unit) {
        return (unit.toMillis(duration) + 49) / 50;
    }

    private void tick(MinecraftServer server) {
        if (stopped) {
            return;
        }
        ticks++;
        for (Task task : List.copyOf(tasks)) {
            if (stopped) {
                break;
            }
            if (task.cancelled || task.server != server || task.nextTick > ticks) {
                continue;
            }
            task.action.run();
            if (task.periodTicks == 0) {
                task.cancelled = true;
            } else {
                task.nextTick = ticks + task.periodTicks;
            }
        }
        tasks.removeIf(task -> task.cancelled);
    }

    public void shutdown() {
        stopped = true;
        tasks.clear();
    }

    public void cancelAll() {
        tasks.clear();
    }

    public interface TaskHandle {
        void cancel();
    }

    private static final class Task {
        private final MinecraftServer server;
        private final Runnable action;
        private final long periodTicks;
        private long nextTick;
        private boolean cancelled;

        private Task(MinecraftServer server, Runnable action, long nextTick, long periodTicks) {
            this.server = server;
            this.action = action;
            this.nextTick = nextTick;
            this.periodTicks = periodTicks;
        }
    }
}
