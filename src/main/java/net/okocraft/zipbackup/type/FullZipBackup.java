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
import java.util.function.Predicate;
import java.util.function.Supplier;

class FullZipBackup implements BackupType {

    private final Supplier<ZipParameters> zipParametersSupplier;
    private final Predicate<Path> shouldIgnore;

    FullZipBackup(@NotNull Supplier<ZipParameters> zipParametersSupplier,
                  @NotNull Predicate<Path> shouldIgnore) {
        this.zipParametersSupplier = zipParametersSupplier;
        this.shouldIgnore = shouldIgnore;
    }

    @Override
    public void backup(@NotNull Path sourceDir, @NotNull Path backupDir) throws Exception {
        FileUtils.createDirectoriesIfNotExists(backupDir);

        var zipPath = FilePathFactory.newBackupFile(backupDir);

        try (var zip = new ZipFile(zipPath.toFile());
             var walk = Files.walk(sourceDir)) {
            walk.forEach(path -> processPath(zip, sourceDir, path));
        }
    }

    private void processPath(@NotNull ZipFile zipFile, @NotNull Path root, @NotNull Path path) {
        if (shouldIgnore.test(path) || !Files.isRegularFile(path)) {
            return;
        }

        try {
            storeFileToZip(zipFile, path, root.relativize(path).toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void storeFileToZip(@NotNull ZipFile zipFile, @NotNull Path file, @NotNull String relative) throws IOException {
        var parameters = zipParametersSupplier.get();
        parameters.setFileNameInZip(relative);
        zipFile.addFile(file.toFile(), parameters);
    }
}
