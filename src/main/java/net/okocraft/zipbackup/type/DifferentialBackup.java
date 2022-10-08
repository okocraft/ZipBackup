package net.okocraft.zipbackup.type;

import com.github.siroshun09.configapi.api.util.FileUtils;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.okocraft.zipbackup.util.FilePathFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;
import java.util.function.Supplier;

class DifferentialBackup implements BackupType {

    private final Predicate<Path> shouldIgnore;
    private final boolean checkFileContent;
    private final Supplier<ZipParameters> zipParametersSupplier;

    DifferentialBackup(@NotNull Supplier<ZipParameters> zipParametersSupplier,
                       @NotNull Predicate<Path> shouldIgnore,
                       boolean checkFileContent) {
        this.zipParametersSupplier = zipParametersSupplier;
        this.shouldIgnore = shouldIgnore;
        this.checkFileContent = checkFileContent;
    }

    @Override
    public void backup(@NotNull Path sourceDir, @NotNull Path backupDir) throws Exception {
        FileUtils.createDirectoriesIfNotExists(backupDir);

        var fullBackupDir = backupDir.resolve("full-backup-" + DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()));

        if (Files.isDirectory(fullBackupDir)) {
            diffBackup(sourceDir, backupDir, fullBackupDir);
        } else {
            fullBackup(sourceDir, fullBackupDir);
        }
    }

    private void fullBackup(@NotNull Path source, @NotNull Path fullBackupDir) throws IOException {
        try (var walk = Files.walk(source)) {
            walk.forEach(file -> copyFile(file, fullBackupDir.resolve(source.relativize(file))));
        }
    }

    private void copyFile(@NotNull Path sourceFile, @NotNull Path targetFile) {
        if (shouldIgnore.test(sourceFile)) {
            return;
        }

        try {
            Files.copy(sourceFile, targetFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void diffBackup(@NotNull Path root, @NotNull Path backupDir, @NotNull Path fullBackupDir) throws IOException {
        try (var zip = new ZipFile(FilePathFactory.newBackupFile(backupDir).toFile());
             var walk = Files.walk(root)) {
            walk.forEach(file -> processFile(zip, root, file, fullBackupDir));
        }
    }

    private void processFile(@NotNull ZipFile zipFile, @NotNull Path root, @NotNull Path path, @NotNull Path fullBackupDir) {
        if (shouldIgnore.test(path) || !Files.isRegularFile(path)) {
            return;
        }

        var relative = root.relativize(path);
        var fullBackupFile = fullBackupDir.resolve(relative);

        try {
            if (shouldBackup(path, fullBackupFile)) {
                storeFileToZip(zipFile, path, relative.toString());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void storeFileToZip(@NotNull ZipFile zipFile, @NotNull Path path, @NotNull String relative) throws IOException {
        var zipParameters = zipParametersSupplier.get();
        zipParameters.setFileNameInZip(relative);
        zipFile.addFile(path.toFile(), zipParameters);
    }

    private boolean shouldBackup(@NotNull Path current, @NotNull Path fullBackup) throws IOException {
        if (!Files.isRegularFile(fullBackup)) {
            return true;
        }

        var currentAttribute = Files.readAttributes(current, BasicFileAttributes.class);
        var fullbackBackupAttribute = Files.readAttributes(fullBackup, BasicFileAttributes.class);

        return currentAttribute.lastModifiedTime().toInstant().isAfter(fullbackBackupAttribute.lastModifiedTime().toInstant()) ||
                currentAttribute.size() != fullbackBackupAttribute.size() ||
                (checkFileContent && Files.mismatch(current, fullBackup) != -1);
    }
}
