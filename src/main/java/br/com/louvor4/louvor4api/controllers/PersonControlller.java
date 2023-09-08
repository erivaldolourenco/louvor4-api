package br.com.louvor4.louvor4api.controllers;

import br.com.louvor4.louvor4api.converter.PersonConverter;
import br.com.louvor4.louvor4api.dto.PersonDTO;
import br.com.louvor4.louvor4api.models.Person;
import br.com.louvor4.louvor4api.services.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/person")
public class PersonControlller {
    @Autowired
    PersonService personService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Person> getAll() {
        return personService.getAll();
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Person getById(@PathVariable(value = "id") UUID id) {
        return personService.getById(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public PersonDTO create(@RequestBody PersonDTO persondto){
        return PersonConverter.INSTANCE.toDto(personService.create(PersonConverter.INSTANCE.toEntity(persondto)));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") UUID id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

