package net.okocraft.zipbackup.task.backup;

import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import net.okocraft.zipbackup.type.BackupType;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class PluginBackupTask implements Runnable {

    private final ZipBackupPlugin plugin;
    private final Path directoryPathCache;

    public PluginBackupTask(@NotNull ZipBackupPlugin plugin) {
        this.plugin = plugin;
        this.directoryPathCache = plugin.getBackupDirectory().resolve("plugins");
    }

    @Override
    public void run() {
        if (Bukkit.isStopping()) {
            return;
        }

        plugin.getLogger().info("Starting backup task for plugins...");
        long start = System.currentTimeMillis();

        var pluginDirectory = plugin.getDataFolder().getParentFile().toPath();

        if (!Files.exists(pluginDirectory)) {
            return;
        }

        BackupType type;

        if (plugin.getConfiguration().get(Settings.BACKUP_DIFFERENTIAL)) {
            boolean checkFileContent = plugin.getConfiguration().get(Settings.BACKUP_CHECK_FILE_CONTENT);
            type = BackupType.differential(plugin::getZipParameters, this::shouldIgnore, checkFileContent);
        } else {
            type = BackupType.full(plugin::getZipParameters, this::shouldIgnore);
        }

        try {
            type.backup(pluginDirectory, directoryPathCache);
        } catch (Exception e) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while backing up to zip",
                    e
            );
            return;
        }

        long end = System.currentTimeMillis();
        plugin.getLogger().info("Backup task for plugins has been finished. (" + (end - start) + "ms)");
    }

    private boolean shouldIgnore(@NotNull Path path) {
        if (path.toAbsolutePath().startsWith(plugin.getBackupDirectory().toAbsolutePath())) {
            return true;
        }

        var strPath = path.toString();

        if (strPath.startsWith("plugins/")) {
            strPath = strPath.substring("plugins/".length());
        }

        if (strPath.startsWith("ZipBackup") && strPath.endsWith(".zip")) {
            return true;
        }

        if (plugin.getConfiguration().get(Settings.BACKUP_PLUGIN_IGNORE_JAR_FILES) && strPath.endsWith(".jar")) {
            return true;
        }

        for (var exclude : plugin.getConfiguration().get(Settings.BACKUP_PLUGIN_EXCLUDE_FOLDERS)) {
            if (strPath.equals(exclude) || strPath.startsWith(exclude)) {
                return true;
            }
        }

        return false;
    }
}
