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
import java.util.UUID;

@RestController
@RequestMapping("/ministries")
public class MinistryControlller {
    @Autowired
    MinistryService ministryService;

//    @Secured({PERMISSION_MANAGER})
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public MinistryDTO createMinistry(@RequestBody @Valid MinistryDTO ministryDTO, Authentication authentication){
        return ministryService.createMinistry(ministryDTO, authentication.getName());
    }

    @DeleteMapping(value="/{idMinistry}",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity deleteMinistry(@PathVariable(value = "idMinistry") UUID idMinistry){
        return ResponseEntity.ok().body("O ministerio "+idMinistry+"foi deletado com sucesso!");
    }

    @GetMapping(value="/{idMinistry}",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public MinistryDTO getMinistry(@PathVariable(value = "idMinistry") UUID idMinistry) {
        return ministryService.getMinistry(idMinistry);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MinistryDTO> getAllMinistries() {
        return ministryService.getAllMinistries();
    }

    @PostMapping(value="/add-member",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity addMemberToMinistry(@RequestBody @Valid MinistryPersonDTO ministryPersonDTO, Authentication authentication){
        ministryService.addMemberToMinistry(ministryPersonDTO.idMinistry(),ministryPersonDTO.idPerson());
        return ResponseEntity.ok().build();
    }

    @PostMapping(value="/new-role",consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity newRole(@RequestBody @Valid MinistryPersonDTO ministryPersonDTO) throws IllegalAccessException {
        ministryService.newRole(ministryPersonDTO);
        return ResponseEntity.ok().build();
    }



}

