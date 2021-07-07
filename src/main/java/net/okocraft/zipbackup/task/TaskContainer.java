package net.okocraft.zipbackup.task;

import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import net.okocraft.zipbackup.task.backup.PluginBackupTask;
import net.okocraft.zipbackup.task.backup.WorldBackupTask;
import net.okocraft.zipbackup.task.purge.BackupPurgeTask;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TaskContainer {

    private final ZipBackupPlugin plugin;
    private ScheduledExecutorService scheduler;

    public TaskContainer(@NotNull ZipBackupPlugin plugin) {
        this.plugin = plugin;
    }

    public void scheduleTasks() {
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        int purgeInterval = plugin.getConfiguration().get(Settings.BACKUP_PURGE_INTERVAL);

        if (0 < purgeInterval) {
            scheduler.scheduleAtFixedRate(
                    new BackupPurgeTask(plugin),
                    purgeInterval,
                    purgeInterval,
                    TimeUnit.MINUTES
            );
        }

        int worldBackupInterval = plugin.getConfiguration().get(Settings.BACKUP_INTERVAL_WORLD);

        if (0 < worldBackupInterval) {
            scheduler.scheduleAtFixedRate(
                    new WorldBackupTask(plugin),
                    worldBackupInterval,
                    worldBackupInterval,
                    TimeUnit.MINUTES
            );
        }

        int pluginBackupInterval = plugin.getConfiguration().get(Settings.BACKUP_PLUGIN_INTERVAL);

        if (0 < pluginBackupInterval) {
            scheduler.scheduleAtFixedRate(
                    new PluginBackupTask(plugin),
                    pluginBackupInterval,
                    pluginBackupInterval,
                    TimeUnit.MINUTES
            );
        }
    }

    public @NotNull CompletableFuture<Void> runPurgeTask() {
        return CompletableFuture.runAsync(new BackupPurgeTask(plugin), scheduler);
    }

    public @NotNull CompletableFuture<Void> runPluginBackupTask() {
        return CompletableFuture.runAsync(new PluginBackupTask(plugin), scheduler);
    }

    public @NotNull CompletableFuture<Void> runWorldBackupTask() {
        return CompletableFuture.runAsync(new WorldBackupTask(plugin), scheduler);
    }

    public @NotNull CompletableFuture<Void> runWorldBackupTask(@NotNull World world) {
        return CompletableFuture.runAsync(
                () -> {
                    long start = System.currentTimeMillis();

                    plugin.getLogger().info("Starting world backup task... (" + world.getName() + ")");
                    WorldBackupTask.backupWorld(world, plugin);

                    long finish = System.currentTimeMillis();
                    long took = finish - start;

                    plugin.getLogger().info("The world backup task has been finished. (" + took + "ms)");
                },
                scheduler
        );
    }

    public void shutdownIfRunning() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}
