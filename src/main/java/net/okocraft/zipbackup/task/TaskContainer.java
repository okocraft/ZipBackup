package net.okocraft.zipbackup.task;

import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import net.okocraft.zipbackup.task.backup.PluginBackupTask;
import net.okocraft.zipbackup.task.backup.WorldBackupTask;
import net.okocraft.zipbackup.task.purge.BackupPurgeTask;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TaskContainer {

    private final ZipBackupPlugin plugin;
    private ScheduledExecutorService scheduler;
    private ExecutorService backupExecutors;

    public TaskContainer(@NotNull ZipBackupPlugin plugin) {
        this.plugin = plugin;
    }

    public void scheduleTasks() {
        if (backupExecutors == null) {
            backupExecutors = Executors.newFixedThreadPool(4);
        }

        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        int purgeInterval = plugin.getConfiguration().get(Settings.BACKUP_PURGE_INTERVAL);

        if (0 < purgeInterval) {
            scheduler.scheduleAtFixedRate(
                    this::runPurgeTask,
                    purgeInterval,
                    purgeInterval,
                    TimeUnit.MINUTES
            );
        }

        int worldBackupInterval = plugin.getConfiguration().get(Settings.BACKUP_INTERVAL_WORLD);

        if (0 < worldBackupInterval) {
            scheduler.scheduleAtFixedRate(
                    () -> runWorldBackupTask().forEach(task -> {}), // call terminal operations
                    worldBackupInterval,
                    worldBackupInterval,
                    TimeUnit.MINUTES
            );
        }

        int pluginBackupInterval = plugin.getConfiguration().get(Settings.BACKUP_PLUGIN_INTERVAL);

        if (0 < pluginBackupInterval) {
            scheduler.scheduleAtFixedRate(
                    this::runPluginBackupTask,
                    pluginBackupInterval,
                    pluginBackupInterval,
                    TimeUnit.MINUTES
            );
        }
    }

    public @NotNull CompletableFuture<Void> runPurgeTask() {
        return runBackupTask(new BackupPurgeTask(plugin));
    }

    public @NotNull CompletableFuture<Void> runPluginBackupTask() {
        return runBackupTask(new PluginBackupTask(plugin));
    }

    public @NotNull Stream<CompletableFuture<Void>> runWorldBackupTask() {
        return createBackupTaskForAllWorlds().map(this::runBackupTask);
    }

    public @NotNull CompletableFuture<Void> runWorldBackupTask(@NotNull World world) {
        return runBackupTask(new WorldBackupTask(plugin, world));
    }

    public void shutdownIfRunning() {
        if (backupExecutors != null && !backupExecutors.isShutdown()) {
            backupExecutors.shutdownNow();
            backupExecutors = null;
        }

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private @NotNull Stream<WorldBackupTask> createBackupTaskForAllWorlds() {
        var excludedWorlds = plugin.getConfiguration().get(Settings.BACKUP_WORLD_EXCLUDE);

        return plugin.getServer().getWorlds()
                .stream()
                .filter(Predicate.not(world -> excludedWorlds.contains(world.getName())))
                .map(world -> new WorldBackupTask(plugin, world));
    }

    private @NotNull CompletableFuture<Void> runBackupTask(@NotNull Runnable task) {
        return CompletableFuture.runAsync(task, backupExecutors);
    }
}
