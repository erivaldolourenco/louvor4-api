package br.com.louvor4.api.controllers;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.services.AudioFileService;
import br.com.louvor4.api.services.SongService;
import br.com.louvor4.api.shared.dto.Audio.AudioFileDTO;
import br.com.louvor4.api.shared.dto.Song.ChordSheetDTO;
import br.com.louvor4.api.shared.dto.Song.ChordSheetEditPermissionDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import br.com.louvor4.api.shared.dto.Song.SongLyricsDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("songs")
public class SongController {

    private final SongService songService;
    private final AudioFileService audioFileService;

    public SongController(SongService songService, AudioFileService audioFileService) {
        this.songService = songService;
        this.audioFileService = audioFileService;
    }

    @GetMapping("/{songId}")
    public ResponseEntity<SongDTO> get(@PathVariable UUID songId) {
        SongDTO dto = songService.get(songId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/create")
    public ResponseEntity<SongDTO> create(@RequestBody @Valid SongDTO createDto) {
        SongDTO dto = songService.create(createDto);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/update")
    public ResponseEntity<SongDTO> update(@RequestBody @Valid SongDTO updateDto) {
        SongDTO dto = songService.update(updateDto);
        return ResponseEntity.ok(dto);
    }

    @PostMapping(value = "/{songId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AudioFileDTO> uploadAudio(
            @PathVariable UUID songId,
            @RequestParam AudioType type,
            @RequestParam("file") MultipartFile file) {
        AudioFileDTO dto = audioFileService.uploadSongAudio(songId, type, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{songId}/delete")
    public ResponseEntity<Void> delete(@PathVariable UUID songId) {
        songService.delete(songId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{songId}/lyrics")
    public ResponseEntity<SongLyricsDTO> getLyrics(@PathVariable UUID songId) {
        SongLyricsDTO dto = songService.getLyrics(songId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{songId}/lyrics")
    public ResponseEntity<SongLyricsDTO> updateLyrics(
            @PathVariable UUID songId,
            @RequestBody @Valid SongLyricsDTO lyricsDto) {
        SongLyricsDTO dto = songService.updateLyrics(songId, lyricsDto.lyrics());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{songId}/chord-sheet")
    public ResponseEntity<ChordSheetDTO> getChordSheet(@PathVariable UUID songId) {
        ChordSheetDTO dto = songService.getChordSheet(songId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{songId}/chord-sheet")
    public ResponseEntity<ChordSheetDTO> updateChordSheet(
            @PathVariable UUID songId,
            @RequestBody @Valid ChordSheetDTO chordSheetDto) {
        ChordSheetDTO dto = songService.updateChordSheet(songId, chordSheetDto.chordSheetJson());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{songId}/chord-sheet")
    public ResponseEntity<Void> deleteChordSheet(@PathVariable UUID songId) {
        songService.deleteChordSheet(songId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{songId}/chord-sheet/import")
    public ResponseEntity<ChordSheetDTO> importChordSheet(
            @PathVariable UUID songId,
            @RequestBody @Valid ChordSheetDTO chordSheetDto) {
        ChordSheetDTO dto = songService.importChordSheet(songId, chordSheetDto.chordSheetJson());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{songId}/chord-sheet/edit-permission")
    public ResponseEntity<ChordSheetEditPermissionDTO> editPermissionChordSheet(
            @PathVariable UUID songId,
            @RequestBody @Valid ChordSheetEditPermissionDTO editPermissionDto) {
        ChordSheetEditPermissionDTO dto = songService.updateChordSheetEditPermission(songId, editPermissionDto.editPermission());
        return ResponseEntity.ok(dto);
    }
}
