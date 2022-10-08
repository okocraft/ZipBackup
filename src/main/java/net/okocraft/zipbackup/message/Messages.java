package net.okocraft.zipbackup.message;

import net.kyori.adventure.text.Component;

import java.util.function.Function;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.AQUA;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public final class Messages {

    public static final Function<String, Component> COMMAND_NO_PERMISSION =
            permission ->
                    text().append(text("You don't have the permission: ", RED))
                            .append(text(permission, AQUA))
                            .build();

    public static final Component COMMAND_USAGE =
            empty().toBuilder()
                    .append(text("/zipbackup backup <world/plugin> {world-name} - Run the backup task.", GRAY))
                    .append(newline())
                    .append(text("/zipbackup purge - Delete expired backups", GRAY))
                    .append(newline())
                    .append(text("/zipbackup reload - Reload config.yml", GRAY))
                    .build();

    public static final Component COMMAND_PURGE_START =
            text("Starting purge task...", GRAY);

    public static final Component COMMAND_PURGE_FINISH =
            text("The purge task has been finished. Please check your console.", AQUA);

    public static final Component COMMAND_BACKUP_PLUGIN_START =
            text("Starting plugin backup task...", GRAY);

    public static final Component COMMAND_BACKUP_PLUGIN_FINISH =
            text("The plugin backup task has been finished. Please check your console.", AQUA);

    public static final Component COMMAND_BACKUP_WORLD_START =
            text("Starting world backup task...", GRAY);

    public static final Component COMMAND_BACKUP_WORLD_FINISH =
            text("The world backup task has been finished. Please check your console.", AQUA);

    public static final Component COMMAND_BACKUP_WORLD_UNKNOWN =
            text("Unknown world: ", RED);

    public static final Component COMMAND_COPY_BACKUP_START =
            text("Creating zipped backup file... (This may take a moment.)", GRAY);

    public static final Function<String, Component> COMMAND_COPY_BACKUP_NOT_FOUND_DATA =
            data -> text("The backup not found: ", RED).append(text(data, AQUA));

    public static final Component COMMAND_COPY_BACKUP_ERROR_OCCURRED =
            text("An error occurred while coping the backup file. Please check your console.", RED);

    public static final Function<String, Component> COMMAND_COPY_BACKUP_INVALID_FILENAME =
            invalid -> text("Invalid filename: ", RED).append(text(invalid, AQUA));

    public static final Function<String, Component> COMMAND_COPY_BACKUP_FULL_BACKUP_NOT_FOUND =
            path -> text("Full backup not found: ", RED).append(text(path, AQUA));

    public static final Function<String, Component> COMMAND_COPY_BACKUP_FINISH =
            path -> text("The requested backup has been copied to " + path);

    public static final Component COMMAND_CURRENTLY_RUNNING =
            text("The command is currently running. Please try again later.", RED);

    public static final Component COMMAND_RELOAD_START = text("Reloading backup plugin...", GRAY);

    public static final Component COMMAND_RELOAD_SUCCESS = text("Successfully reloaded the plugin.", AQUA);

    public static final Component COMMAND_RELOAD_FAILURE =
            text("Failed to reload the plugin. Please check your console.", RED);

    public Messages() {
        throw new UnsupportedOperationException();
    }
}
