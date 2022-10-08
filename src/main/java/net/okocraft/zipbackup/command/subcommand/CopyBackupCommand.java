package net.okocraft.zipbackup.command.subcommand;

import com.github.siroshun09.mccommand.common.AbstractCommand;
import com.github.siroshun09.mccommand.common.CommandResult;
import com.github.siroshun09.mccommand.common.context.CommandContext;
import com.github.siroshun09.mccommand.common.filter.StringFilter;
import com.github.siroshun09.mccommand.common.sender.Sender;
import net.lingala.zip4j.ZipFile;
import net.okocraft.zipbackup.ZipBackupPlugin;
import net.okocraft.zipbackup.config.Settings;
import net.okocraft.zipbackup.message.Messages;
import net.okocraft.zipbackup.util.FilePathFactory;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CopyBackupCommand extends AbstractCommand {

    private final ZipBackupPlugin plugin;

    public CopyBackupCommand(@NotNull ZipBackupPlugin plugin) {
        super("copybackup", "zipbackup.command.copybackup", Set.of("cb"));
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

        if (arguments.size() < 3) {
            sender.sendMessage(Messages.COMMAND_USAGE);
            return CommandResult.NO_ARGUMENT;
        }

        var dataName = arguments.get(1).get();
        var backupDir = plugin.getBackupDirectory().resolve(dataName);

        if (Files.notExists(backupDir)) {
            sender.sendMessage(Messages.COMMAND_COPY_BACKUP_NOT_FOUND_DATA.apply(dataName));
            return CommandResult.INVALID_ARGUMENTS;
        }

        var file = arguments.get(2).get();
        var filePath = backupDir.resolve(file);

        if (Files.notExists(filePath)) {
            sender.sendMessage(Messages.COMMAND_COPY_BACKUP_NOT_FOUND_DATA.apply(filePath.toString()));
            return CommandResult.INVALID_ARGUMENTS;
        }

        sender.sendMessage(Messages.COMMAND_COPY_BACKUP_START);

        var sourceFilename = filePath.getFileName().toString();

        var targetPath = plugin.getDataFolder().toPath().resolve(dataName + "-" + sourceFilename + (sourceFilename.endsWith(".zip") ? "" : ".zip"));
        CommandResult result;

        if (plugin.getConfiguration().get(Settings.BACKUP_DIFFERENTIAL)) {
            if (Files.isDirectory(filePath)) {
                result = zipDirectory(filePath, targetPath, sender);
            } else {
                result = createZip(filePath, targetPath, sender);
            }
        } else {
            result = copyZip(filePath, targetPath, sender);
        }

        if (result == CommandResult.SUCCESS) {
            sender.sendMessage(Messages.COMMAND_COPY_BACKUP_FINISH.apply(targetPath.toAbsolutePath().toString()));
        }

        return result;
    }

    private @NotNull CommandResult copyZip(@NotNull Path source, @NotNull Path target, @NotNull Sender sender) {
        try {
            Files.copy(source, target);
        } catch (IOException e) {
            reportException(sender, "Could not copy " + source.toAbsolutePath() + " to " + target.toAbsolutePath(), e);
            sender.sendMessage(Messages.COMMAND_COPY_BACKUP_ERROR_OCCURRED);
            return CommandResult.EXCEPTION_OCCURRED;
        }

        return CommandResult.SUCCESS;
    }

    private @NotNull CommandResult zipDirectory(@NotNull Path source, @NotNull Path target, @NotNull Sender sender) {
        try (var zip = new ZipFile(target.toFile())) {
            var param = plugin.getZipParameters();
            param.setIncludeRootFolder(false);
            zip.addFolder(source.toFile(), param);
        } catch (IOException e) {
            reportException(sender, "Could not zip " + source.toAbsolutePath(), e);
            sender.sendMessage(Messages.COMMAND_COPY_BACKUP_ERROR_OCCURRED);
            return CommandResult.EXCEPTION_OCCURRED;
        }

        return CommandResult.SUCCESS;
    }

    private @NotNull CommandResult createZip(@NotNull Path source, @NotNull Path target, @NotNull Sender sender) {
        var workDir = prepareWorkDir(sender);
        var fullBackupDir = obtainFullBackupDir(source, sender);

        if (workDir == null || fullBackupDir == null) {
            return CommandResult.STATE_ERROR;
        }

        var copiedFullBackupDir = copyFullBackup(fullBackupDir, workDir, sender);
        var extractDir = extractDiffBackup(source, workDir, sender);

        if (copiedFullBackupDir == null || extractDir == null) {
            return CommandResult.EXCEPTION_OCCURRED;
        }

        var dirToZip = mergeBackup(fullBackupDir, extractDir, sender);

        if (dirToZip == null) {
            return CommandResult.EXCEPTION_OCCURRED;
        }

        var result = zipDirectory(dirToZip, target, sender);

        deleteWorkDir(workDir, sender);

        return result;
    }

    private @Nullable Path prepareWorkDir(@NotNull Sender sender) {
        var workDir = plugin.getDataFolder().toPath().resolve(".work");

        try {
            Files.createDirectories(workDir);
        } catch (IOException e) {
            reportException(sender, "Could not create working directory: " + workDir.toAbsolutePath(), e);
            return null;
        }

        return workDir;
    }

    private void deleteWorkDir(@NotNull Path workDir, @NotNull Sender sender) {
        try {
            deleteDirectory(workDir);
        } catch (Exception e) {
            reportException(sender, "Could not delete directory: " + workDir.toAbsolutePath(), e);
        }
    }

    private @Nullable Path obtainFullBackupDir(@NotNull Path source, @NotNull Sender sender) {
        LocalDate date;
        try {
            date = LocalDateTime.parse(
                    source.getFileName().toString().replace(".zip", ""),
                    FilePathFactory.FILENAME_FORMAT
            ).toLocalDate();
        } catch (DateTimeParseException e) {
            sender.sendMessage(Messages.COMMAND_COPY_BACKUP_INVALID_FILENAME.apply(source.getFileName().toString()));
            return null;
        }

        var fullBackupDir = source.getParent().resolve("full-backup-" + DateTimeFormatter.ISO_LOCAL_DATE.format(date));

        if (!Files.isDirectory(fullBackupDir)) {
            sender.sendMessage(Messages.COMMAND_COPY_BACKUP_FULL_BACKUP_NOT_FOUND.apply(fullBackupDir.toAbsolutePath().toString()));
            return null;
        }

        return fullBackupDir;
    }

    private @Nullable Path copyFullBackup(@NotNull Path fullBackupDir, @NotNull Path workDir, @NotNull Sender sender) {
        var dest = workDir.resolve("full-backup");

        try (var walk = Files.walk(fullBackupDir)) {
            walk.forEach(file -> copyFile(file, dest.resolve(fullBackupDir.relativize(file))));
        } catch (Exception e) {
            reportException(sender, "Could not copy full backup to directory", e);
            return null;
        }

        return dest;
    }

    private @Nullable Path extractDiffBackup(@NotNull Path source, @NotNull Path workDir, @NotNull Sender sender) {
        var extractDir = workDir.resolve("extract");

        try (var zip = new ZipFile(source.toFile())) {
            zip.extractAll(Path.of(".").relativize(extractDir).toString());
        } catch (Exception e) {
            reportException(sender, "Could not extract zip: " + source.toAbsolutePath(), e);
            return null;
        }

        return extractDir;
    }

    private @Nullable Path mergeBackup(@NotNull Path fullBackupDir, @NotNull Path extractDir, @NotNull Sender sender) {
        try (var walk = Files.walk(extractDir)) {
            walk.forEach(path -> moveFile(path, fullBackupDir.resolve(extractDir.relativize(path))));
        } catch (Exception e) {
            reportException(sender, "Could not move backup files to " + fullBackupDir.toAbsolutePath(), e);
            return null;
        }

        return fullBackupDir;
    }

    private void copyFile(@NotNull Path sourceFile, @NotNull Path targetFile) {
        try {
            Files.copy(sourceFile, targetFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void moveFile(@NotNull Path sourceFile, @NotNull Path targetFile) {
        try {
            if (Files.isRegularFile(sourceFile)) {
                Files.move(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void deleteDirectory(@NotNull Path directory) throws Exception {
        try (var walk = Files.walk(directory)) {
            walk.sorted(Comparator.reverseOrder()).forEach(this::deleteFile);
        }
    }

    private void deleteFile(@NotNull Path path) {
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void reportException(@NotNull Sender sender, @NotNull String message, @NotNull Exception e) {
        sender.sendMessage(Messages.COMMAND_COPY_BACKUP_ERROR_OCCURRED);
        plugin.getLogger().log(Level.SEVERE, message, e);
    }

    @Override
    public @NotNull List<String> onTabCompletion(@NotNull CommandContext context) {
        var arguments = context.getArguments();

        if (!context.getSender().hasPermission(getPermission()) || arguments.size() < 2) {
            return Collections.emptyList();
        }

        var secondArgument = arguments.get(1).get();

        if (arguments.size() == 2) {
            var candidate = plugin.getServer().getWorlds().stream().map(World::getName).collect(Collectors.toCollection(ArrayList::new));
            candidate.add("plugins");

            return candidate.stream()
                    .filter(StringFilter.startsWith(secondArgument))
                    .toList();
        }

        var backupDir = plugin.getBackupDirectory().resolve(secondArgument);

        if (arguments.size() == 3 && Files.isDirectory(backupDir)) {
            var thirdArgument = arguments.get(2).get();

            try (var list = Files.list(backupDir)) {
                return list.map(Path::getFileName)
                        .map(Path::toString)
                        .filter(StringFilter.endsWith(".zip").or(StringFilter.startsWith("full-backup-")))
                        .filter(StringFilter.startsWith(thirdArgument))
                        .toList();
            } catch (IOException e) {
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }
}
