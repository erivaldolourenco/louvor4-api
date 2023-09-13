package br.com.louvor4.louvor4api.controllers;

import br.com.louvor4.louvor4api.converter.PersonConverter;
import br.com.louvor4.louvor4api.dto.MinistryDTO;
import br.com.louvor4.louvor4api.dto.PersonDTO;
import br.com.louvor4.louvor4api.exceptions.NotFoundException;
import br.com.louvor4.louvor4api.models.Person;
import br.com.louvor4.louvor4api.services.MinistryService;
import br.com.louvor4.louvor4api.services.PersonService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ministry")
public class MinistryControlller {
    @Autowired
    MinistryService ministryService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public MinistryDTO create(@RequestBody @Valid MinistryDTO ministryDTO){
        return ministryService.create(ministryDTO);
    }

}

