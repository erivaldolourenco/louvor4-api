package br.com.louvor4.louvor4api.services;

import br.com.louvor4.louvor4api.exceptions.NotFoundException;
import br.com.louvor4.louvor4api.models.Person;
import br.com.louvor4.louvor4api.repositories.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class PersonService {
    private Logger logger = Logger.getLogger(PersonService.class.getName());

    @Autowired
    PersonRepository personRepository;

    public List<Person> getAll() {
        logger.info("Listando todas as pessoas!");
        return personRepository.findAll();
    }

    public Person getById(UUID id) {
        return personRepository.findById(id).orElseThrow(() -> new NotFoundException("Não foi encontrado pessoa come esse ID."));
    }

    public Person create(Person pessoa) {
        logger.info("Nova pessoa criada!");
        return personRepository.save(pessoa);
    }

    public void delete(UUID id) {
        Person person = personRepository.findById(id).orElseThrow(() -> new NotFoundException("Não foi encontrado pessoa come esse ID."));
        personRepository.delete(person);
    }
}
