package net.okocraft.zipbackup.command.subcommand;

import com.github.siroshun09.mccommand.common.AbstractCommand;
import com.github.siroshun09.mccommand.common.CommandResult;
import com.github.siroshun09.mccommand.common.context.CommandContext;
import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.message.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class PurgeCommand extends AbstractCommand {

    private final ZipBackupPlugin plugin;

    public PurgeCommand(@NotNull ZipBackupPlugin plugin) {
        super("purge", "zipbackup.command.purge", Collections.emptySet());
        this.plugin = plugin;
    }

    @Override
    public @NotNull CommandResult onExecution(@NotNull CommandContext context) {
        var sender = context.getSender();

        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(Messages.COMMAND_NO_PERMISSION.apply(getPermission()));
            return CommandResult.NO_PERMISSION;
        }

        sender.sendMessage(Messages.COMMAND_PURGE_START);

        plugin.getTaskContainer().runPurgeTask().join();

        sender.sendMessage(Messages.COMMAND_PURGE_FINISH);

        return CommandResult.SUCCESS;
    }

    @Override
    public @NotNull List<String> onTabCompletion(@NotNull CommandContext context) {
        return Collections.emptyList();
    }
}
