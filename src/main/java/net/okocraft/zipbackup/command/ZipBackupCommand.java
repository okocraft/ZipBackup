package net.okocraft.zipbackup.command;

import com.github.siroshun09.mccommand.common.AbstractCommand;
import com.github.siroshun09.mccommand.common.Command;
import com.github.siroshun09.mccommand.common.CommandResult;
import com.github.siroshun09.mccommand.common.SubCommandHolder;
import com.github.siroshun09.mccommand.common.context.CommandContext;
import com.github.siroshun09.mccommand.common.filter.StringFilter;
import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.command.subcommand.BackupCommand;
import net.okocraft.zipbackup.command.subcommand.CopyBackupCommand;
import net.okocraft.zipbackup.command.subcommand.PurgeCommand;
import net.okocraft.zipbackup.command.subcommand.ReloadCommand;
import net.okocraft.zipbackup.message.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ZipBackupCommand extends AbstractCommand {

    private final AtomicBoolean runningCommand = new AtomicBoolean(false);
    private final ZipBackupPlugin plugin;
    private final SubCommandHolder subCommandHolder;

    public ZipBackupCommand(@NotNull ZipBackupPlugin plugin) {
        super("zipbackup", "zipbackup.command", Set.of("zb", "zbu", "zbackup"));
        this.plugin = plugin;
        this.subCommandHolder =
                SubCommandHolder.of(
                        new BackupCommand(plugin),
                        new CopyBackupCommand(plugin),
                        new PurgeCommand(plugin),
                        new ReloadCommand(plugin)
                );

    }

    @Override
    public @NotNull CommandResult onExecution(@NotNull CommandContext context) {
        var sender = context.getSender();

        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(Messages.COMMAND_NO_PERMISSION.apply(getPermission()));
            return CommandResult.NO_PERMISSION;
        }

        if (runningCommand.get()) {
            sender.sendMessage(Messages.COMMAND_CURRENTLY_RUNNING);
            return CommandResult.STATE_ERROR;
        }

        var args = context.getArguments();

        if (args.isEmpty()) {
            sender.sendMessage(Messages.COMMAND_USAGE);
            return CommandResult.NO_ARGUMENT;
        }

        var subCommand = subCommandHolder.search(args.get(0));

        if (subCommand != null) {
            runningCommand.set(true);

            plugin.getCommandExecutor().submit(() -> {
                subCommand.onExecution(context);
                runningCommand.set(false);
            });

            return CommandResult.SUCCESS; // dummy result
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
                    .toList();
        }

        var subcommand = subCommandHolder.search(firstArgument);

        if (subcommand != null) {
            return subcommand.onTabCompletion(context);
        } else {
            return Collections.emptyList();
        }
    }
}
