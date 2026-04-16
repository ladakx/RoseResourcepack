package me.ladakx.roserp;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import com.cjcrafter.foliascheduler.TaskImplementation;
import org.bukkit.entity.Entity;

import java.util.concurrent.TimeUnit;

public final class SchedulerAdapter {

    private final ServerImplementation server;

    public SchedulerAdapter(RoseRP plugin) {
        this.server = new FoliaCompatibility(plugin).getServerImplementation();
    }

    public boolean isOwnedByCurrentRegion(Entity entity) {
        return server.isOwnedByCurrentRegion(entity);
    }

    public TaskImplementation<Void> runGlobal(Runnable runnable) {
        return server.global().run(runnable);
    }

    public TaskImplementation<Void> runGlobalTimer(Runnable runnable, long initialDelayTicks, long periodTicks) {
        return server.global().runAtFixedRate(runnable, initialDelayTicks, periodTicks);
    }

    public TaskImplementation<Void> runAsync(Runnable runnable) {
        return server.async().runNow(runnable);
    }

    public TaskImplementation<Void> runAsyncTimer(Runnable runnable, long initialDelay, long period, TimeUnit unit) {
        return server.async().runAtFixedRate(runnable, initialDelay, period, unit);
    }

    public TaskImplementation<Void> runEntity(Entity entity, Runnable runnable) {
        return server.entity(entity).run(runnable);
    }

    public TaskImplementation<Void> runEntity(Entity entity, Runnable runnable, Runnable retired) {
        return server.entity(entity).run(runnable, retired);
    }

    public TaskImplementation<Void> runEntityTimer(
            Entity entity, Runnable runnable, Runnable retired, long initialDelayTicks, long periodTicks) {
        return server.entity(entity).runAtFixedRate(runnable, retired, initialDelayTicks, periodTicks);
    }

    public void cancelTasks() {
        server.cancelTasks();
    }
}
