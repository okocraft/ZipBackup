package net.okocraft.zipbackup.task.backup;

import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import net.okocraft.zipbackup.type.BackupType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class WorldBackupTask implements Runnable {

    private static final String SESSION_FILE_NAME = "session.lock";
    private static final String OLD_FILE_SUFFIX = "_old";

    private final ZipBackupPlugin plugin;
    private final World world;

    public WorldBackupTask(@NotNull ZipBackupPlugin plugin, @NotNull World world) {
        this.plugin = plugin;
        this.world = world;
    }

    @Override
    public void run() {
        var worldName = world.getName();

        plugin.getLogger().info("Starting backup task for world " + worldName);

        long start = System.currentTimeMillis();

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
        BackupType type;

        if (plugin.getConfiguration().get(Settings.BACKUP_DIFFERENTIAL)) {
            boolean checkFileContent = plugin.getConfiguration().get(Settings.BACKUP_CHECK_FILE_CONTENT);
            type = BackupType.differential(plugin::getZipParameters, this::shouldBeIgnored, checkFileContent);
        } else {
            type = BackupType.full(plugin::getZipParameters, this::shouldBeIgnored);
        }

        try {
            type.backup(world.getWorldFolder().toPath(), directory);
        } catch (Exception e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while creating the backup (" + worldName + ")",
                    e
            );
            return;
        }

        long end = System.currentTimeMillis();
        plugin.getLogger().info("Backup task for world " + worldName + " has been finished. (" + (end - start) + "ms)");
    }

    private boolean shouldBeIgnored(@NotNull Path file) {
        var name = file.getFileName().toString();
        return name.equals(SESSION_FILE_NAME) || name.endsWith(OLD_FILE_SUFFIX);
    }
}
