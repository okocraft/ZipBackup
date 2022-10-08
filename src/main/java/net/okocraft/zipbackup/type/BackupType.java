package net.okocraft.zipbackup.type;

import net.lingala.zip4j.model.ZipParameters;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface BackupType {

    static @NotNull BackupType full(@NotNull Supplier<ZipParameters> zipParametersSupplier,
                                    @NotNull Predicate<Path> shouldIgnore) {
        return new FullZipBackup(zipParametersSupplier, shouldIgnore);
    }

    static @NotNull BackupType differential(@NotNull Supplier<ZipParameters> zipParametersSupplier,
                                            @NotNull Predicate<Path> shouldIgnore,
                                            boolean checkFileContent) {
        return new DifferentialBackup(zipParametersSupplier, shouldIgnore, checkFileContent);
    }

    void backup(@NotNull Path sourceDir, @NotNull Path backupDir) throws Exception;
}
