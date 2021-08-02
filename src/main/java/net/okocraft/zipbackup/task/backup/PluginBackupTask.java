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

        try (var zipFile = new ZipFile(zipPath.toFile())) {
            zipFile.addFolder(pluginDirectory.toFile(), zipParameter);
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

        var shouldIgnoreJar = plugin.getConfiguration().get(Settings.BACKUP_PLUGIN_IGNORE_JAR_FILES);

        return file -> {
            for (File excluded : excludedSet) {
                if (excluded.equals(file)) {
                    return true;
                }
            }

            if (shouldIgnoreJar) {
                return file.getName().endsWith(".jar");
            }

            return false;
        };
    }
}
