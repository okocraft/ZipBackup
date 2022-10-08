package net.okocraft.zipbackup.util;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class FilePathFactory {

    public static final DateTimeFormatter FILENAME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");

    public static @NotNull Path newBackupFile(@NotNull Path directory) {
        return directory.resolve(FILENAME_FORMAT.format(LocalDateTime.now()) + ".zip");
    }
}
