package net.okocraft.zipbackup.util;

import org.bukkit.Bukkit;

public final class FoliaChecker {

    private static final boolean FOLIA;

    static {
        boolean isFolia;

        try {
            //noinspection JavaReflectionMemberAccess
            Bukkit.class.getDeclaredMethod("getAsyncScheduler");
            isFolia = true;
        } catch (NoSuchMethodException e) {
            isFolia = false;
        }

        FOLIA = isFolia;
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    private FoliaChecker() {
        throw new UnsupportedOperationException();
    }
}
