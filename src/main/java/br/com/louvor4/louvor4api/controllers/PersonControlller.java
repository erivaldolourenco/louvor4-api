package br.com.louvor4.louvor4api.controllers;

import br.com.louvor4.louvor4api.shared.dto.MinistryDTO;
import br.com.louvor4.louvor4api.shared.dto.PersonDTO;
import br.com.louvor4.louvor4api.exceptions.NotFoundException;
import br.com.louvor4.louvor4api.models.Person;
import br.com.louvor4.louvor4api.services.PersonService;
import br.com.louvor4.louvor4api.shared.projection.MemberProjection;
import br.com.louvor4.louvor4api.shared.projection.PersonProjection;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static br.com.louvor4.louvor4api.shared.constants.Messages.EMAIL_ESTA_JA_OCUPADO;
import static br.com.louvor4.louvor4api.util.ReturnUtil.convertOrThrow;

@RestController
@RequestMapping("/people")
public class PersonControlller {
    @Autowired
    PersonService personService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PersonDTO createPerson(@RequestBody @Valid PersonDTO personDto) {
        if (personService.findByLogin(personDto.email())) {
            throw new NotFoundException(EMAIL_ESTA_JA_OCUPADO);
        }
        return personService.createPerson(personDto);
    }

    @GetMapping(value = "/{idPerson}")
    public PersonDTO getPerson(@PathVariable(value = "idPerson") UUID idPerson) {
        return personService.getPersonById(idPerson);
    }

    @DeleteMapping(value = "/{idPerson}")
    public ResponseEntity<?> delete(@PathVariable(value = "idPerson") UUID idPerson) {
        personService.deletePerson(idPerson);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PersonProjection>> getAllPeople() {
        List<PersonDTO> persons = personService.getAllPeople();
        return convertOrThrow(PersonProjection.class, persons);
    }


    @GetMapping(value = "/{idPerson}/ministries", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MinistryDTO> getMinistryOfLoggedPerson(@PathVariable(value = "idPerson") UUID idPerson) {
        return personService.getMinistries(idPerson);
    }
}

