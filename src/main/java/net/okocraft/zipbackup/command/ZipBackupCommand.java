package net.okocraft.zipbackup.command;

import com.github.siroshun09.mccommand.common.AbstractCommand;
import com.github.siroshun09.mccommand.common.Command;
import com.github.siroshun09.mccommand.common.CommandResult;
import com.github.siroshun09.mccommand.common.SubCommandHolder;
import com.github.siroshun09.mccommand.common.context.CommandContext;
import com.github.siroshun09.mccommand.common.filter.StringFilter;
import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.command.subcommand.BackupCommand;
import net.okocraft.zipbackup.command.subcommand.PurgeCommand;
import net.okocraft.zipbackup.message.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ZipBackupCommand extends AbstractCommand {

    private final SubCommandHolder subCommandHolder;

    public ZipBackupCommand(@NotNull ZipBackupPlugin plugin) {
        super("zipbackup", "zipbackup.command", Set.of("zb", "zbu", "zbackup"));
        this.subCommandHolder =
                SubCommandHolder.of(
                        new BackupCommand(plugin),
                        new PurgeCommand(plugin)
                );
    }

    @Override
    public @NotNull CommandResult onExecution(@NotNull CommandContext context) {
        var sender = context.getSender();

        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(Messages.COMMAND_NO_PERMISSION.apply(getPermission()));
            return CommandResult.NO_PERMISSION;
        }

        var args = context.getArguments();

        if (args.isEmpty()) {
            sender.sendMessage(Messages.COMMAND_USAGE);
            return CommandResult.NO_ARGUMENT;
        }

        var subCommand = subCommandHolder.search(args.get(0));

        if (subCommand != null) {
            return subCommand.onExecution(context);
        } else {
            sender.sendMessage(Messages.COMMAND_USAGE);
            return CommandResult.INVALID_ARGUMENTS;
        }
    }

    @Override
    public @NotNull List<String> onTabCompletion(@NotNull CommandContext context) {
        var sender = context.getSender();

        if (!sender.hasPermission(getPermission())) {
            return Collections.emptyList();
        }

        var args = context.getArguments();

        if (args.isEmpty()) {
            return Collections.emptyList();
        }

        var firstArgument = args.get(0).get();

        if (args.size() == 1) {
            return subCommandHolder.getSubCommands()
                    .stream()
                    .filter(cmd -> sender.hasPermission(cmd.getPermission()))
                    .map(Command::getName)
                    .filter(StringFilter.startsWith(firstArgument))
                    .collect(Collectors.toUnmodifiableList());
        }

        var subcommand = subCommandHolder.search(firstArgument);

        if (subcommand != null) {
            return subcommand.onTabCompletion(context);
        } else {
            return Collections.emptyList();
        }
    }
}
