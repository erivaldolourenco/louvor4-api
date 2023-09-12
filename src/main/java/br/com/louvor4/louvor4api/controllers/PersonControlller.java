package br.com.louvor4.louvor4api.controllers;

import br.com.louvor4.louvor4api.converter.PersonConverter;
import br.com.louvor4.louvor4api.dto.PersonDTO;
import br.com.louvor4.louvor4api.exceptions.NotFoundException;
import br.com.louvor4.louvor4api.models.Person;
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
    public PersonDTO create(@RequestBody @Valid PersonDTO persondto){
        if(personService.findByLogin(persondto.getEmail())){
            throw new NotFoundException("Já existe um Usuario com este email!");
        }
        String encryptedPassword = new BCryptPasswordEncoder().encode(persondto.getPassword());
        Person person =  PersonConverter.INSTANCE.toEntity(persondto);
        person.setPassword(encryptedPassword);
        return PersonConverter.INSTANCE.toDto(personService.create(person));
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> delete(@PathVariable(value = "id") UUID id) {
        personService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

