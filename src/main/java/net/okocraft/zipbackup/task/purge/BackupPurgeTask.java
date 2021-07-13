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
import java.util.stream.Collectors;

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
        long start = System.currentTimeMillis();
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

        long end = System.currentTimeMillis();

        var log =
                deleted.intValue() == 0 ?
                        "No expired files has been deleted." :
                        "Expired files (" + deleted.get() + ") has been deleted.";

        System.out.println(log + " (" + (end - start) + "ms)");

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

        int max = plugin.getConfiguration().get(Settings.BACKUP_PURGE_MAXIMUM_FILES);
        boolean shouldDelete = false;

        try (var currentFiles = Files.list(directory)) {
            var count = currentFiles.count();

            if (count == 0) {
                Files.deleteIfExists(directory);
                return deleted.intValue();
            } else {
                shouldDelete = max < count;
            }
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while deleting directory (" + directory.toAbsolutePath() + ")",
                    exception
            );
        }

        if (shouldDelete) {
            try (var list = Files.list(directory)) {
                var files =
                        list.map(BackupFile::new)
                                .sorted()
                                .map(BackupFile::getPath)
                                .collect(Collectors.toList());

                while (!files.isEmpty() && max < files.size()) {
                    var path = files.get(0);

                    if (Files.isRegularFile(path)) {
                        Files.delete(path);
                        deleted.incrementAndGet();
                    }

                    files.remove(0);
                }
            } catch (IOException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "An error occurred while deleting file.",
                        exception
                );
            }
        }

        return deleted.intValue();
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

    private static class BackupFile implements Comparable<BackupFile> {

        private final Path path;
        private long lastModification;

        private BackupFile(@NotNull Path path) {
            this.path = path;

            try {
                this.lastModification = Files.getLastModifiedTime(path).toMillis();
            } catch (IOException ignored) {
                this.lastModification = System.currentTimeMillis();
            }
        }

        public @NotNull Path getPath() {
            return path;
        }

        @Override
        public int compareTo(@NotNull BackupFile other) {
            return Long.compare(lastModification, other.lastModification);
        }
    }
}
