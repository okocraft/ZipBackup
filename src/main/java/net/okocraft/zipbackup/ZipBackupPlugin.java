package net.okocraft.zipbackup;

import com.github.siroshun09.configapi.api.util.FileUtils;
import com.github.siroshun09.configapi.api.util.ResourceUtils;
import com.github.siroshun09.configapi.yaml.YamlConfiguration;
import com.github.siroshun09.mccommand.paper.PaperCommandFactory;
import com.github.siroshun09.mccommand.paper.listener.AsyncTabCompleteListener;
import net.lingala.zip4j.model.ZipParameters;
import net.okocraft.zipbackup.command.ZipBackupCommand;
import net.okocraft.zipbackup.config.Settings;
import net.okocraft.zipbackup.listener.ServerStartListener;
import net.okocraft.zipbackup.task.TaskContainer;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class ZipBackupPlugin extends JavaPlugin {

    private final YamlConfiguration configuration =
            YamlConfiguration.create(getDataFolder().toPath().resolve("config.yml"));

    private final TaskContainer taskContainer = new TaskContainer(this);
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();

    private Path backupDirectory;
    private ZipParameters zipParameters;

    @Override
    public void onEnable() {
        try {
            reload();
        } catch (Throwable throwable) {
            getLogger().log(
                    Level.SEVERE,
                    "An error occurred while enabling plugin",
                    throwable
            );
            getServer().getPluginManager().disablePlugin(this);
        }

        getServer().getPluginManager().registerEvents(new ServerStartListener(this), this);

        var command = new ZipBackupCommand(this);
        PaperCommandFactory.registerIfExists(this, command);
        AsyncTabCompleteListener.register(this, command);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        taskContainer.shutdownIfRunning();
        commandExecutor.shutdownNow();
    }

    public void reload() throws Exception {
        FileUtils.createDirectoriesIfNotExists(getDataFolder().toPath());
        ResourceUtils.copyFromJarIfNotExists(getFile().toPath(), "config.yml", configuration.getPath());

        configuration.reload();

        prepareBackupDirectory();

        prepareZipParameters();

        taskContainer.shutdownIfRunning();
        taskContainer.scheduleTasks();
    }

    public @NotNull YamlConfiguration getConfiguration() {
        return configuration;
    }

    public @NotNull TaskContainer getTaskContainer() {
        return taskContainer;
    }

    public @NotNull Path getBackupDirectory() {
        return backupDirectory;
    }

    @Contract(" -> new")
    public @NotNull ZipParameters getZipParameters() {
        return new ZipParameters(zipParameters);
    }

    public @NotNull ExecutorService getCommandExecutor() {
        return commandExecutor;
    }

    private void prepareBackupDirectory() {
        var backupDirectoryPath = configuration.get(Settings.BACKUP_DIRECTORY);

        if (backupDirectoryPath.isEmpty()) {
            backupDirectory = getDataFolder().toPath().resolve("backups");
        } else {
            backupDirectory = Path.of(backupDirectoryPath);
        }
    }

    private void prepareZipParameters() {
        zipParameters = new ZipParameters();
        zipParameters.setCompressionLevel(configuration.get(Settings.COMPRESSION_LEVEL));
    }
}
