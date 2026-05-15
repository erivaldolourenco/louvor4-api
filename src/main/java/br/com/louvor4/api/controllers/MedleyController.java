package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.MedleyService;
import br.com.louvor4.api.shared.dto.Medley.CreateMedleyRequest;
import br.com.louvor4.api.shared.dto.Medley.MedleyResponse;
import br.com.louvor4.api.shared.dto.Medley.UpdateMedleyRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/medleys")
public class MedleyController {

    private final MedleyService medleyService;

    public MedleyController(MedleyService medleyService) {
        this.medleyService = medleyService;
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
}
