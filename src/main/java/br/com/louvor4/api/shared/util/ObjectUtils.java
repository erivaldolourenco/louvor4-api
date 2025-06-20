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
}
