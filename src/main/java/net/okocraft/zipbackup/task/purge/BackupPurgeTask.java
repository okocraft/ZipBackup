package net.okocraft.zipbackup.task.purge;

import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class BackupPurgeTask implements Runnable {

    private final ZipBackupPlugin plugin;

    public BackupPurgeTask(@NotNull ZipBackupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!Files.exists(plugin.getBackupDirectory())) {
            return;
        }

        plugin.getLogger().info("Starting delete expired backups task...");
        var deleted = new AtomicInteger(0);

        try (var list = Files.list(plugin.getBackupDirectory())) {
            list.filter(Files::isDirectory)
                    .map(this::checkBackups)
                    .forEach(deleted::addAndGet);
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while deleting files.",
                    exception
            );
        }

        if (deleted.intValue() == 0) {
            plugin.getLogger().info("No expired files has been deleted.");
        } else {
            plugin.getLogger().info("Expired files has been deleted. (" + deleted.get() + ")");
        }
    }

    private int checkBackups(@NotNull Path directory) {
        var deleted = new AtomicInteger(0);

        try (var list = Files.list(directory)) {
            list.filter(Files::isRegularFile)
                    .filter(this::isExpired)
                    .forEach(path -> {
                        try {
                            deleted.incrementAndGet();
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            exception.printStackTrace();
                        }
                    });
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while deleting file.",
                    exception
            );
            return deleted.intValue();
        }

        try (var currentList = Files.list(directory)) {
            if (currentList.count() == 0) {
                Files.deleteIfExists(directory);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while deleting directory (" + directory.toAbsolutePath() + ")",
                    exception
            );
        }

        return deleted.get();
    }

    private boolean isExpired(@NotNull Path path) {
        try {
            return plugin.getConfiguration().get(Settings.BACKUP_PURGE_EXPIRATION_DAYS)
                    <= Duration.between(Files.getLastModifiedTime(path).toInstant(), Instant.now()).toDays();
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check file, ignore " + path.toAbsolutePath(), exception);
            return false;
        }
    }
}
