package br.com.louvor4.api.services.external.music.spotify;

import br.com.louvor4.api.enums.MusicProvider;
import br.com.louvor4.api.exceptions.ExternalMusicException;
import br.com.louvor4.api.services.external.music.SearchMusicService;
import br.com.louvor4.api.services.external.music.spotify.dto.SpotifyArtist;
import br.com.louvor4.api.services.external.music.spotify.dto.SpotifySearchResponse;
import br.com.louvor4.api.services.external.music.spotify.dto.SpotifyTrack;
import br.com.louvor4.api.shared.dto.ExternalMusic.ExternalMusicDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

@Service
public class SearchMusicSpotifyService implements SearchMusicService {

    private static final Logger log = LoggerFactory.getLogger(SearchMusicSpotifyService.class);

    private final RestClient spotifyApiRestClient;
    private final SpotifyTokenService spotifyTokenService;

    public SearchMusicSpotifyService(RestClient spotifyApiRestClient, SpotifyTokenService spotifyTokenService) {
        this.spotifyApiRestClient = spotifyApiRestClient;
        this.spotifyTokenService = spotifyTokenService;
    }

    @Override
    public List<ExternalMusicDTO> search(String term, int limit) {
        try {
            SpotifySearchResponse response = spotifyApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", term)
                            .queryParam("type", "track")
                            .queryParam("limit", limit)
                            .queryParam("market", "BR")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + spotifyTokenService.getAccessToken())
                    .retrieve()
                    .body(SpotifySearchResponse.class);

            if (response == null || response.tracks() == null || response.tracks().items() == null) {
                return List.of();
            }

            return response.tracks().items().stream()
                    .map(this::toExternalMusicDTO)
                    .toList();
        } catch (RestClientException ex) {
            log.error("Falha ao buscar música no Spotify: {}", ex.getMessage(), ex);
            throw new ExternalMusicException("Falha ao buscar música no Spotify.", ex);
        }
    }

    @Override
    public Optional<String> findUrlByIsrc(String isrc) {
        if (isrc == null || isrc.isBlank()) {
            return Optional.empty();
        }

        try {
            SpotifySearchResponse response = spotifyApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", "isrc:" + isrc)
                            .queryParam("type", "track")
                            .queryParam("limit", 1)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + spotifyTokenService.getAccessToken())
                    .retrieve()
                    .body(SpotifySearchResponse.class);

            if (response == null || response.tracks() == null || response.tracks().items() == null
                    || response.tracks().items().isEmpty()) {
                return Optional.empty();
            }

            SpotifyTrack track = response.tracks().items().get(0);
            return track.externalUrls() == null
                    ? Optional.empty()
                    : Optional.ofNullable(track.externalUrls().spotify());
        } catch (RestClientException ex) {
            log.warn("Falha ao buscar música no Spotify pelo ISRC {}: {}", isrc, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public MusicProvider getProvider() {
        return MusicProvider.SPOTIFY;
    }

    private ExternalMusicDTO toExternalMusicDTO(SpotifyTrack track) {
        String artist = (track.artists() == null || track.artists().isEmpty())
                ? null
                : track.artists().stream().map(SpotifyArtist::name).findFirst().orElse(null);

        String album = track.album() == null ? null : track.album().name();

        String coverUrl = (track.album() == null || track.album().images() == null || track.album().images().isEmpty())
                ? null
                : track.album().images().get(0).url();

        String isrc = track.externalIds() == null ? null : track.externalIds().isrc();
        String spotifyUrl = track.externalUrls() == null ? null : track.externalUrls().spotify();

        return new ExternalMusicDTO(
                track.id(),
                track.name(),
                artist,
                album,
                coverUrl,
                track.durationMs(),
                track.previewUrl(),
                spotifyUrl,
                null,
                isrc,
                null,
                MusicProvider.SPOTIFY
        );
    }
}
