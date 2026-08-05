package br.com.louvor4.api.services.external.music.deezer;

import br.com.louvor4.api.enums.MusicProvider;
import br.com.louvor4.api.exceptions.ExternalMusicException;
import br.com.louvor4.api.services.external.music.SearchMusicService;
import br.com.louvor4.api.services.external.music.deezer.dto.DeezerSearchResponse;
import br.com.louvor4.api.services.external.music.deezer.dto.DeezerTrack;
import br.com.louvor4.api.shared.dto.ExternalMusic.ExternalMusicDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Service
public class SearchMusicDeezerService implements SearchMusicService {

    private static final Logger log = LoggerFactory.getLogger(SearchMusicDeezerService.class);

    private final RestClient deezerRestClient;

    public SearchMusicDeezerService(RestClient deezerRestClient) {
        this.deezerRestClient = deezerRestClient;
    }

    @Override
    public List<ExternalMusicDTO> search(String term, int limit) {
        try {
            DeezerSearchResponse response = deezerRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", term)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(DeezerSearchResponse.class);

            if (response == null || response.data() == null) {
                return List.of();
            }

            return response.data().stream()
                    .map(this::toExternalMusicDTO)
                    .toList();
        } catch (RestClientException ex) {
            log.error("Falha ao buscar música no Deezer: {}", ex.getMessage(), ex);
            throw new ExternalMusicException("Falha ao buscar música no Deezer.", ex);
        }
    }

    @Override
    public Optional<String> findUrlByIsrc(String isrc) {
        if (isrc == null || isrc.isBlank()) {
            return Optional.empty();
        }

        try {
            DeezerTrack track = deezerRestClient.get()
                    .uri("/track/isrc:{isrc}", isrc)
                    .retrieve()
                    .body(DeezerTrack.class);

            return track == null ? Optional.empty() : Optional.ofNullable(track.link());
        } catch (RestClientException ex) {
            log.warn("Falha ao buscar música no Deezer pelo ISRC {}: {}", isrc, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public MusicProvider getProvider() {
        return MusicProvider.DEEZER;
    }

    private ExternalMusicDTO toExternalMusicDTO(DeezerTrack track) {
        String artist = track.artist() == null ? null : track.artist().name();
        String album = track.album() == null ? null : track.album().title();
        String coverUrl = track.album() == null ? null : track.album().cover();
        Long durationMs = track.duration() == null ? null : track.duration() * 1000L;

        return new ExternalMusicDTO(
                track.id() == null ? null : String.valueOf(track.id()),
                track.title(),
                artist,
                album,
                coverUrl,
                durationMs,
                track.preview(),
                null,
                track.link(),
                track.isrc(),
                null,
                MusicProvider.DEEZER
        );
    }
}
