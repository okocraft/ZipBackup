package net.okocraft.zipbackup.task.purge;

import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class BackupPurgeTask implements Runnable {

    private final ZipBackupPlugin plugin;

    public BackupPurgeTask(@NotNull ZipBackupPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (Bukkit.isStopping() || !Files.exists(plugin.getBackupDirectory())) {
            return;
        }

        plugin.getLogger().info("Starting delete expired backups task...");
        long start = System.currentTimeMillis();
        var deleted = new AtomicInteger(0);

        try (var list = Files.list(plugin.getBackupDirectory())) {
            list.filter(Files::isDirectory)
                    .map(this::checkBackups)
                    .forEach(deleted::addAndGet);
        } catch (Exception exception) {
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

        plugin.getLogger().info(log + " (" + (end - start) + "ms)");
    }

    private int checkBackups(@NotNull Path directory) {
        var deleted = new AtomicInteger(0);

        try (var list = Files.list(directory)) {
            list.forEach(path -> processPath(deleted, path));
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while deleting file.",
                    exception
            );
        }

        return deleted.intValue();
    }

    private void processPath(@NotNull AtomicInteger counter, @NotNull Path path) {
        if (Files.isDirectory(path)) {
            processDirectory(counter, path);
            return;
        }

        if (Files.isRegularFile(path)) {
            processFile(counter, path);
        }
    }

    private void processDirectory(@NotNull AtomicInteger counter, @NotNull Path path) {
        if (!plugin.getConfiguration().get(Settings.BACKUP_DIFFERENTIAL)) {
            return;
        }

        var filename = path.getFileName().toString();

        if (!filename.startsWith("full-backup-")) {
            return;
        }

        LocalDate date;

        try {
            date = LocalDate.parse(filename.substring("full-backup-".length()));
        } catch (DateTimeParseException e) {
            return;
        }

        if (plugin.getConfiguration().get(Settings.BACKUP_PURGE_EXPIRATION_DAYS)
                <= date.until(LocalDate.now()).getDays()) {
            try (var walk = Files.walk(path)) {
                walk.sorted(Comparator.reverseOrder()).forEach(this::deleteFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            counter.incrementAndGet();
        }
    }

    private void processFile(@NotNull AtomicInteger counter, @NotNull Path file) {
        if (isExpired(file)) {
            try {
                Files.delete(file);
                counter.incrementAndGet();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private boolean isExpired(@NotNull Path path) {
        try {
            return plugin.getConfiguration().get(Settings.BACKUP_PURGE_EXPIRATION_DAYS)
                    <= Duration.between(Files.getLastModifiedTime(path).toInstant().truncatedTo(ChronoUnit.DAYS), Instant.now().truncatedTo(ChronoUnit.DAYS)).toDays();
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check file, ignore " + path.toAbsolutePath(), exception);
            return false;
        }
    }

    private void deleteFile(@NotNull Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
