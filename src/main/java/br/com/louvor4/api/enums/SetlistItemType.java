package br.com.louvor4.api.enums;

import java.util.Set;

public enum SetlistItemType {
    SONG,
    MEDLEY,
    PROJECT_SONG;

    /** Tipos que representam uma música avulsa no repertório (pessoal ou do projeto). */
    public static final Set<SetlistItemType> SONG_TYPES = Set.of(SONG, PROJECT_SONG);
}
