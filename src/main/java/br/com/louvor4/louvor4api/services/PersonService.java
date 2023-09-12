package br.com.louvor4.louvor4api.services;

import br.com.louvor4.louvor4api.exceptions.NotFoundException;
import br.com.louvor4.louvor4api.models.Permission;
import br.com.louvor4.louvor4api.models.Person;
import br.com.louvor4.louvor4api.repositories.PermissionRepository;
import br.com.louvor4.louvor4api.repositories.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class PersonService {
    private Logger logger = Logger.getLogger(PersonService.class.getName());

    @Autowired
    PersonRepository personRepository;

    @Autowired
    PermissionRepository permissionRepository;

    public List<Person> getAll() {
        logger.info("Listando todas as pessoas!");
        return personRepository.findAll();
    }

    public Person getById(UUID id) {
        return personRepository.findById(id).orElseThrow(() -> new NotFoundException("Não foi encontrado pessoa come esse ID."));
    }

    public Person create(Person person) {
        logger.info("Nova pessoa criada!");
        person.setAccountNonExpired(true);
        person.setAccountNonLocked(true);
        person.setCredentialsNonExpired(true);
        person.setEnabled(true);
        person.setPermissions(getDefaultPermission());
        return personRepository.save(person);
    }

    private List<Permission> getDefaultPermission(){
        List<Permission> permissions =  new ArrayList<>();
        permissions.add(permissionRepository.findByDescription("MEMBER"));
        return permissions;
    }

    public void delete(UUID id) {
        Person person = personRepository.findById(id).orElseThrow(() -> new NotFoundException("Não foi encontrado pessoa come esse ID."));
        personRepository.delete(person);
    }

    public Boolean findByLogin(String email) {
        logger.info("Verificando se email existe na base!");
        return personRepository.findByEmail(email) != null ? true: false;
    }
    public UserDetails getPersonUserDetails(String email) {
        return personRepository.findByEmail(email);
    }
}
