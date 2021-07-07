package net.okocraft.zipbackup.task.backup;

import com.github.siroshun09.configapi.api.util.FileUtils;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ExcludeFileFilter;
import net.lingala.zip4j.model.ZipParameters;
import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import net.okocraft.zipbackup.util.FilePathFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PluginBackupTask implements Runnable {

    private final ZipBackupPlugin plugin;
    private final Path directoryPathCache;

    public PluginBackupTask(@NotNull ZipBackupPlugin plugin) {
        this.plugin = plugin;
        this.directoryPathCache = plugin.getBackupDirectory().resolve("plugins");
    }

    @Override
    public void run() {
        plugin.getLogger().info("Starting plugin backup task...");
        long start = System.currentTimeMillis();

        var pluginDirectory = plugin.getDataFolder().getParentFile().toPath();

        if (!Files.exists(pluginDirectory)) {
            return;
        }

        var zipParameter = new ZipParameters(plugin.getZipParameters());

        zipParameter.setExcludeFileFilter(createFileFilter(pluginDirectory));

        var zipPath = FilePathFactory.newBackupFile(directoryPathCache);

        try {
            FileUtils.createDirectoriesIfNotExists(zipPath.getParent());
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while creating the directory (" + zipPath.getParent() + ")",
                    exception
            );
            return;
        }

        var zipFile = new ZipFile(zipPath.toFile());

        try (var fileList = Files.list(pluginDirectory)) {
            fileList.forEach(p -> addToZip(p, zipFile, zipParameter));
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while backing up to zip",
                    exception
            );
        }

        long end = System.currentTimeMillis();
        plugin.getLogger().info("Plugin backup task has been finished. (" + (end - start) + "ms)");
    }

    private @NotNull ExcludeFileFilter createFileFilter(@NotNull Path pluginDir) {
        var excludedSet =
                plugin.getConfiguration().get(Settings.BACKUP_PLUGIN_EXCLUDE_FOLDERS)
                        .stream()
                        .map(pluginDir::resolve)
                        .map(Path::toFile)
                        .collect(Collectors.toSet());

        excludedSet.add(plugin.getBackupDirectory().toFile());

        return file -> {
            for (File excluded : excludedSet) {
                if (excluded.equals(file)) {
                    return true;
                }
            }

            return false;
        };
    }

    private void addToZip(@NotNull Path path, @NotNull ZipFile target, @NotNull ZipParameters parameters) {
        try {
            if (Files.isRegularFile(path)) {
                target.addFile(path.toFile(), parameters);
                return;
            }

            if (Files.isDirectory(path)) {
                target.addFolder(path.toFile(), parameters);
            }
        } catch (IOException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "An error occurred while adding to zip (" + path + ")",
                    exception
            );
        }
    }
}
