package net.okocraft.zipbackup.command.subcommand;

import com.github.siroshun09.mccommand.common.AbstractCommand;
import com.github.siroshun09.mccommand.common.CommandResult;
import com.github.siroshun09.mccommand.common.context.CommandContext;
import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.message.Messages;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

public class ReloadCommand extends AbstractCommand {

    private final ZipBackupPlugin plugin;

    public ReloadCommand(@NotNull ZipBackupPlugin plugin) {
        super("reload", "zipbackup.command.reload", Collections.emptySet());
        this.plugin = plugin;
    }

    @Override
    public @NotNull CommandResult onExecution(@NotNull CommandContext context) {
        var sender = context.getSender();

        if (!sender.hasPermission(getPermission())) {
            sender.sendMessage(Messages.COMMAND_NO_PERMISSION.apply(getPermission()));
            return CommandResult.NO_PERMISSION;
        }

        sender.sendMessage(Messages.COMMAND_RELOAD_START);

        try {
            plugin.reload();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not reload the plugin", e);
            sender.sendMessage(Messages.COMMAND_RELOAD_FAILURE);
            return CommandResult.EXCEPTION_OCCURRED;
        }

        sender.sendMessage(Messages.COMMAND_RELOAD_SUCCESS);
        return CommandResult.SUCCESS;
    }

    @Override
    public @NotNull List<String> onTabCompletion(@NotNull CommandContext context) {
        return Collections.emptyList();
    }
}
