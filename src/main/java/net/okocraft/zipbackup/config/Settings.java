package net.okocraft.zipbackup.config;

import com.github.siroshun09.configapi.common.value.ConfigValue;
import net.lingala.zip4j.model.enums.CompressionLevel;

import java.util.List;

public final class Settings {

    public static final ConfigValue<String> BACKUP_DIRECTORY = config -> config.getString("backup.directory");

    public static final ConfigValue<CompressionLevel> COMPRESSION_LEVEL =
            config -> {
                try {
                    return CompressionLevel.valueOf(config.getString("backup.zip-compression-level").toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    return CompressionLevel.NORMAL;
                }
            };

    public static final ConfigValue<Integer> BACKUP_PLUGIN_INTERVAL =
            config -> config.getInteger("backup.plugin.interval", 60);

    public static final ConfigValue<Boolean> BACKUP_PLUGIN_AFTER_START_UP =
            config -> config.getBoolean("backup.plugin.backup-after-startup", true);

    public static final ConfigValue<List<String>> BACKUP_PLUGIN_EXCLUDE_FOLDERS =
            config -> config.getStringList("backup.plugin.exclude-folders");

    public static final ConfigValue<Integer> BACKUP_INTERVAL_WORLD =
            config -> config.getInteger("backup.world.interval", 60);

    public static final ConfigValue<Boolean> BACKUP_WORLD_AFTER_START_UP =
            config -> config.getBoolean("backup.world.backup-after-startup", true);

    public static final ConfigValue<List<String>> BACKUP_WORLD_EXCLUDE =
            config -> config.getStringList("backup.world.exclude-worlds");

    public static final ConfigValue<Integer> BACKUP_PURGE_INTERVAL =
            config -> config.getInteger("backup.purge.check-interval", 720);

    public static final ConfigValue<Boolean> BACKUP_PURGE_AFTER_STARTUP =
            config -> config.getBoolean("backup.purge.purge-after-startup", true);

    public static final ConfigValue<Integer> BACKUP_PURGE_EXPIRATION_DAYS =
            config -> config.getInteger("backup.purge.expiration-days", 7);

}
