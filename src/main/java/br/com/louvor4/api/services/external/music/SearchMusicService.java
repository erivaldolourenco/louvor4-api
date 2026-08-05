package br.com.louvor4.api.services.external.music;

import br.com.louvor4.api.enums.MusicProvider;
import br.com.louvor4.api.shared.dto.ExternalMusic.ExternalMusicDTO;

import java.util.List;
import java.util.Optional;

public interface SearchMusicService {

    List<ExternalMusicDTO> search(String term, int limit);

    Optional<String> findUrlByIsrc(String isrc);

    MusicProvider getProvider();
}
