package net.okocraft.zipbackup.task.backup;

import com.github.siroshun09.configapi.api.util.FileUtils;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import net.okocraft.zipbackup.util.FilePathFactory;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WorldBackupTask implements Runnable {

    private static final String SESSION_FILE_NAME = "session.lock";
    private static final String OLD_FILE_SUFFIX = "_old";

    private final ZipBackupPlugin plugin;

    public WorldBackupTask(@NotNull ZipBackupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (Bukkit.isStopping()) {
            return;
        }

        plugin.getLogger().info("Starting world backup task...");
        long start = System.currentTimeMillis();

        var excludedWorlds = plugin.getConfiguration().get(Settings.BACKUP_WORLD_EXCLUDE);

        plugin.getServer().getWorlds()
                .stream()
                .filter(world -> !excludedWorlds.contains(world.getName()))
                .forEach(this::backupWorld);

        long end = System.currentTimeMillis();
        plugin.getLogger().info("World backup task has been finished. (" + (end - start) + "ms)");
    }

    public void backupWorld(@NotNull World world) {
        backupWorld(world, plugin);
    }

    public static void backupWorld(@NotNull World world, @NotNull ZipBackupPlugin plugin) {
        var worldName = world.getName();

        if (plugin.getConfiguration().get(Settings.BACKUP_WORLD_SAVE_BEFORE_BACKUP)) {
            var mainThread = Bukkit.getScheduler().getMainThreadExecutor(plugin);
            var saveTask = CompletableFuture.runAsync(world::save, mainThread);

            try {
                saveTask.join();
            } catch (Exception exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "An error occurred while saving the world (" + worldName + ")",
                        exception
                );
                return;
            }
        }

        var directory = plugin.getBackupDirectory().resolve(worldName);

        try {
            FileUtils.createDirectoriesIfNotExists(directory);
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while creating the directory (" + directory + ")",
                    exception
            );
            return;
        }

        var zipPath = FilePathFactory.newBackupFile(directory);
        var parameters = new ZipParameters(plugin.getZipParameters());
        parameters.setExcludeFileFilter(WorldBackupTask::shouldBeIgnored);

        try (var zip = new ZipFile(zipPath.toFile())) {
            zip.addFolder(world.getWorldFolder(), parameters);
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while creating the backup (" + worldName + ")",
                    exception
            );
        }
    }

    private static boolean shouldBeIgnored(@NotNull File file) {
        var name = file.getName();
        return name.equals(SESSION_FILE_NAME) || name.endsWith(OLD_FILE_SUFFIX);
    }
}
