package br.com.louvor4.api.controllers;

import br.com.louvor4.api.enums.MusicProvider;
import br.com.louvor4.api.services.external.music.SearchMusicServiceResolver;
import br.com.louvor4.api.shared.dto.ExternalMusic.ExternalMusicDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("external-music")
public class ExternalMusicController {

    private final SearchMusicServiceResolver searchMusicServiceResolver;

    public ExternalMusicController(SearchMusicServiceResolver searchMusicServiceResolver) {
        this.searchMusicServiceResolver = searchMusicServiceResolver;
    }

    @GetMapping("/search")
    public ResponseEntity<List<ExternalMusicDTO>> search(
            @RequestParam("q") String term,
            @RequestParam(required = false) MusicProvider provider,
            @RequestParam(defaultValue = "10") int limit) {

        List<ExternalMusicDTO> results = provider == null
                ? searchMusicServiceResolver.searchWithFallback(term, limit)
                : searchMusicServiceResolver.search(provider, term, limit);

        return ResponseEntity.ok(results);
    }

    @PostMapping("/select")
    public ResponseEntity<ExternalMusicDTO> select(@RequestBody ExternalMusicDTO music) {
        return ResponseEntity.ok(searchMusicServiceResolver.enrichWithCounterpartUrl(music));
    }
}
