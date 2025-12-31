package br.com.louvor4.api.shared.util;

import org.springframework.web.multipart.MultipartFile;

public final class ObjectUtils {

    /**
     * Checks if a MultipartFile is not null and not empty.
     *
     * @param file the MultipartFile to check
     * @return true if the file is not null and not empty, false otherwise
     */
    public static boolean isNotNullOrEmpty(MultipartFile file) {
        return file != null && !file.isEmpty();
    }
    public static boolean isNotNullOrEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    public static boolean isNotNull(String value) {
        return value != null;
    }

    public static <E extends Enum<E>> boolean isNotNull(E value) {
        return value != null;
    }

}
