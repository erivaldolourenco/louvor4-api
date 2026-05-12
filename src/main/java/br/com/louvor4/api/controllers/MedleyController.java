package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.MedleyService;
import br.com.louvor4.api.shared.dto.Medley.CreateMedleyRequest;
import br.com.louvor4.api.shared.dto.Medley.MedleyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    
}
