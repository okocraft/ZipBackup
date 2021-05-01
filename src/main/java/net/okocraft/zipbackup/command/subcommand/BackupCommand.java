package net.okocraft.zipbackup.command.subcommand;

import com.github.siroshun09.mccommand.common.AbstractCommand;
import com.github.siroshun09.mccommand.common.CommandResult;
import com.github.siroshun09.mccommand.common.argument.Argument;
import com.github.siroshun09.mccommand.common.context.CommandContext;
import com.github.siroshun09.mccommand.common.filter.StringFilter;
import com.github.siroshun09.mccommand.common.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.message.Messages;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BackupCommand extends AbstractCommand {

    private static final Set<String> SECOND_ARGUMENTS = Set.of("plugin", "world");
    private final ZipBackupPlugin plugin;

    public BackupCommand(@NotNull ZipBackupPlugin plugin) {
        super("backup", "zipbackup.command.backup", Set.of("b", "bu"));
        this.plugin = plugin;
    }

    @Override
    public @NotNull CommandResult onExecution(@NotNull CommandContext context) {
        var sender = context.getSender();

        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(Messages.COMMAND_NO_PERMISSION.apply(getPermission()));
            return CommandResult.NO_PERMISSION;
        }

        var arguments = context.getArguments();

        if (arguments.size() < 2) {
            sender.sendMessage(Messages.COMMAND_USAGE);
            return CommandResult.NO_ARGUMENT;
        }

        var secondArgument = arguments.get(1);

        switch (secondArgument.get().toLowerCase()) {
            case "plugin":
                return backupPlugin(sender);
            case "world":
                return backupWorld(sender, arguments);
            default:
                sender.sendMessage(Messages.COMMAND_USAGE);
                return CommandResult.INVALID_ARGUMENTS;
        }
    }

    @Override
    public @NotNull List<String> onTabCompletion(@NotNull CommandContext context) {
        if (!context.getSender().hasPermission(getPermission())) {
            return Collections.emptyList();
        }

        var arguments = context.getArguments();

        if (arguments.size() == 1) {
            return Collections.emptyList();
        }

        var secondArgument = arguments.get(1).get();

        if (arguments.size() == 2) {
            return SECOND_ARGUMENTS.stream()
                    .filter(StringFilter.startsWith(secondArgument))
                    .collect(Collectors.toUnmodifiableList());
        }

        if (arguments.size() == 3 && secondArgument.equalsIgnoreCase("world")) {
            var thirdArgument = arguments.get(2).get();
            return plugin.getServer().getWorlds().stream()
                    .map(World::getName)
                    .filter(StringFilter.startsWith(thirdArgument))
                    .collect(Collectors.toUnmodifiableList());
        }

        return Collections.emptyList();
    }

    private @NotNull CommandResult backupPlugin(@NotNull Sender sender) {
        sender.sendMessage(Messages.COMMAND_BACKUP_PLUGIN_START);

        plugin.getTaskContainer().runPluginBackupTask()
                .thenRun(() -> sender.sendMessage(Messages.COMMAND_BACKUP_PLUGIN_FINISH));

        return CommandResult.SUCCESS;
    }

    private @NotNull CommandResult backupWorld(@NotNull Sender sender, @NotNull List<Argument> arguments) {
        if (arguments.size() < 3) {
            sender.sendMessage(Messages.COMMAND_BACKUP_WORLD_START);

            plugin.getTaskContainer().runWorldBackupTask()
                    .thenRun(() -> sender.sendMessage(Messages.COMMAND_BACKUP_WORLD_FINISH));
        }

        var thirdArgument = arguments.get(2).get();

        var world = plugin.getServer().getWorld(thirdArgument);

        if (world != null) {
            sender.sendMessage(Messages.COMMAND_BACKUP_WORLD_START);

            plugin.getTaskContainer().runWorldBackupTask(world)
                    .thenRun(() -> sender.sendMessage(Messages.COMMAND_BACKUP_WORLD_FINISH));

            return CommandResult.SUCCESS;
        } else {
            sender.sendMessage(
                    Messages.COMMAND_BACKUP_WORLD_UNKNOWN
                            .append(Component.text(thirdArgument, NamedTextColor.AQUA))
            );

            return CommandResult.INVALID_ARGUMENTS;
        }
    }
}