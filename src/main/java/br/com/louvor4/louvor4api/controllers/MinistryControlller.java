package br.com.louvor4.louvor4api.controllers;

import br.com.louvor4.louvor4api.dto.MinistryDTO;
import br.com.louvor4.louvor4api.dto.MinistryPersonDTO;
import br.com.louvor4.louvor4api.services.MinistryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ministry")
public class MinistryControlller {
    @Autowired
    MinistryService ministryService;


    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MinistryDTO> getAll() {
        return ministryService.getAll();
    }

//    @Secured({"LEADER", "ADMIN"})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public MinistryDTO createMinistry(@RequestBody @Valid MinistryDTO ministryDTO, Authentication authentication){
        return ministryService.create(ministryDTO, authentication.getName());
    }

    @PostMapping(value="/add-member",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addMemberToMinistry(@RequestBody @Valid MinistryPersonDTO ministryPersonDTO, Authentication authentication){
        ministryService.addMemberToMinistry(ministryPersonDTO.idMinistry(),ministryPersonDTO.idPerson());
        return ResponseEntity.ok().build();
    }


}

