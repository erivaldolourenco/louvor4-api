package br.com.louvor4.api.controllers;

import br.com.louvor4.api.enums.AudioType;
import br.com.louvor4.api.services.AudioFileService;
import br.com.louvor4.api.services.MedleyService;
import br.com.louvor4.api.shared.dto.Audio.AudioFileDTO;
import br.com.louvor4.api.shared.dto.Medley.CreateMedleyRequest;
import br.com.louvor4.api.shared.dto.Medley.MedleyResponse;
import br.com.louvor4.api.shared.dto.Medley.UpdateMedleyRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/medleys")
public class MedleyController {

    private final MedleyService medleyService;
    private final AudioFileService audioFileService;

    public MedleyController(MedleyService medleyService, AudioFileService audioFileService) {
        this.medleyService = medleyService;
        this.audioFileService = audioFileService;
    }

    @PostMapping("/create")
    public ResponseEntity<MedleyResponse> create(@RequestBody @Valid CreateMedleyRequest request) {
        MedleyResponse created = medleyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<MedleyResponse> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateMedleyRequest request) {
        MedleyResponse updated = medleyService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        medleyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{medleyId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AudioFileDTO> uploadAudio(
            @PathVariable UUID medleyId,
            @RequestParam AudioType type,
            @RequestParam("file") MultipartFile file) {
        AudioFileDTO dto = audioFileService.uploadMedleyAudio(medleyId, type, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
