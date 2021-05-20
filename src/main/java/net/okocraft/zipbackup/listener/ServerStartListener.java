package net.okocraft.zipbackup.listener;

import com.destroystokyo.paper.event.server.ServerTickStartEvent;
import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ServerStartListener implements Listener {

    private final ZipBackupPlugin plugin;

    public ServerStartListener(@NotNull ZipBackupPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStarted(@NotNull ServerTickStartEvent event) {
        boolean backupPlugin = plugin.getConfiguration().get(Settings.BACKUP_PLUGIN_AFTER_START_UP);

        if (backupPlugin) {
            plugin.getTaskContainer().runPluginBackupTask();
        }

        boolean backupWorld = plugin.getConfiguration().get(Settings.BACKUP_WORLD_AFTER_START_UP);

        if (backupWorld) {
            plugin.getTaskContainer().runWorldBackupTask();
        }

        boolean purgeBackup = plugin.getConfiguration().get(Settings.BACKUP_PURGE_AFTER_STARTUP);

        if (purgeBackup) {
            plugin.getTaskContainer().runPurgeTask();
        }

        event.getHandlers().unregister(this);
    }
}
