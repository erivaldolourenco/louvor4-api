package br.com.louvor4.api.services.external.music;

import br.com.louvor4.api.enums.MusicProvider;
import br.com.louvor4.api.exceptions.ExternalMusicException;
import br.com.louvor4.api.shared.dto.ExternalMusic.ExternalMusicDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SearchMusicServiceResolver {

    private static final Logger log = LoggerFactory.getLogger(SearchMusicServiceResolver.class);

    private final Map<MusicProvider, SearchMusicService> strategy;

    public SearchMusicServiceResolver(List<SearchMusicService> serviceList) {
        this.strategy = serviceList.stream()
                .collect(Collectors.toMap(
                        SearchMusicService::getProvider,
                        Function.identity()
                ));
    }

    public List<ExternalMusicDTO> search(MusicProvider provider, String term, int limit) {
        return resolve(provider).search(term, limit);
    }

    public List<ExternalMusicDTO> searchWithFallback(String term, int limit) {
        List<ExternalMusicDTO> deezerResults = tryProvider(MusicProvider.DEEZER, term, limit);
        if (!deezerResults.isEmpty()) {
            return deezerResults;
        }

        return tryProvider(MusicProvider.SPOTIFY, term, limit);
    }

    public Optional<String> findUrlByIsrc(MusicProvider provider, String isrc) {
        try {
            return resolve(provider).findUrlByIsrc(isrc);
        } catch (ExternalMusicException ex) {
            log.warn("Falha ao buscar link em {} pelo ISRC {}: {}", provider, isrc, ex.getMessage());
            return Optional.empty();
        }
    }

    public ExternalMusicDTO enrichWithCounterpartUrl(ExternalMusicDTO music) {
        if (music.isrc() == null || music.isrc().isBlank()) {
            return music;
        }

        MusicProvider counterpart = music.provider() == MusicProvider.DEEZER
                ? MusicProvider.SPOTIFY
                : MusicProvider.DEEZER;
        String counterpartUrl = findUrlByIsrc(counterpart, music.isrc()).orElse(null);

        String spotifyUrl = music.provider() == MusicProvider.DEEZER ? counterpartUrl : music.spotifyUrl();
        String deezerUrl = music.provider() == MusicProvider.SPOTIFY ? counterpartUrl : music.deezerUrl();

        return new ExternalMusicDTO(
                music.externalId(),
                music.title(),
                music.artist(),
                music.album(),
                music.coverUrl(),
                music.durationMs(),
                music.previewUrl(),
                spotifyUrl,
                deezerUrl,
                music.isrc(),
                music.bpm(),
                music.provider()
        );
    }

    private List<ExternalMusicDTO> tryProvider(MusicProvider provider, String term, int limit) {
        try {
            return resolve(provider).search(term, limit);
        } catch (ExternalMusicException ex) {
            log.warn("Falha ao buscar música em {} durante fallback automático: {}", provider, ex.getMessage());
            return List.of();
        }
    }

    private SearchMusicService resolve(MusicProvider provider) {
        SearchMusicService serviceProvider = strategy.get(provider);

        if (serviceProvider == null) {
            throw new ExternalMusicException("Provedor de música não suportado: " + provider);
        }

        return serviceProvider;
    }
}
