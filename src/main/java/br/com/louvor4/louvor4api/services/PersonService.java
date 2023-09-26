package br.com.louvor4.louvor4api.services;

import br.com.louvor4.louvor4api.converter.MinistryConverter;
import br.com.louvor4.louvor4api.converter.PersonConverter;
import br.com.louvor4.louvor4api.dto.MinistryDTO;
import br.com.louvor4.louvor4api.dto.PersonDTO;
import br.com.louvor4.louvor4api.exceptions.NotFoundException;
import br.com.louvor4.louvor4api.models.Ministry;
import br.com.louvor4.louvor4api.models.Permission;
import br.com.louvor4.louvor4api.models.Person;
import br.com.louvor4.louvor4api.repositories.PermissionRepository;
import br.com.louvor4.louvor4api.repositories.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static br.com.louvor4.louvor4api.shared.constants.Messages.MINISTERIO_NAO_ENCONTRADO;
import static br.com.louvor4.louvor4api.shared.constants.Messages.PESSOA_NAO_ENCONTRADA;

@Service
public class PersonService {
    private Logger logger = Logger.getLogger(PersonService.class.getName());

    @Autowired
    PersonRepository personRepository;

    @Autowired
    PermissionRepository permissionRepository;

    public PersonDTO createPerson(PersonDTO personDto) {
        String encryptedPassword = new BCryptPasswordEncoder().encode(personDto.getPassword());
        Person person = PersonConverter.INSTANCE.toEntity(personDto);
        person.setPassword(encryptedPassword);
        logger.info("Nova pessoa criada!");
        person.setAccountNonExpired(true);
        person.setAccountNonLocked(true);
        person.setCredentialsNonExpired(true);
        person.setEnabled(true);
        person.setAppPermissions(getDefaultPermission());
        return PersonConverter.INSTANCE.toDto(personRepository.save(person));
    }

    public void deletePerson(UUID id) {
        logger.info("Deletando pessoa com id: " + id.toString());
        Person person = personRepository.findById(id).orElseThrow(() -> new NotFoundException(PESSOA_NAO_ENCONTRADA));
        personRepository.delete(person);
    }

    public PersonDTO getPersonById(UUID idPerson) {
        logger.info("Consultando pessoa com id: " + idPerson.toString());
        Person person = personRepository.findById(idPerson).orElseThrow(() -> new NotFoundException(PESSOA_NAO_ENCONTRADA));
        return PersonConverter.INSTANCE.toDto(person);
    }

    public List<Person> getAllPeople() {
        logger.info("Listando todas as pessoas!");
        return personRepository.findAll();
    }

    private List<Permission> getDefaultPermission() {
        List<Permission> permissions = new ArrayList<>();
        permissions.add(permissionRepository.findByDescription("USER"));
        return permissions;
    }

    public Boolean findByLogin(String email) {
        logger.info("Verificando se email existe na base!");
        return personRepository.findByEmail(email) != null ? true : false;
    }

    public UserDetails getPersonUserDetails(String email) {
        return personRepository.findByEmail(email);
    }

    public List<MinistryDTO> getMinistries(UUID idPerson) {
        Person person = personRepository.findById(idPerson).orElseThrow(() -> new NotFoundException(PESSOA_NAO_ENCONTRADA));
        List<Ministry> ministryList = person.getMembers().stream().map(member -> member.getMinistry()).collect(Collectors.toList());
        if (ministryList != null && ministryList.size() != 0) {
            return MinistryConverter.INSTANCE.toDto(ministryList);
        } else {
            throw new NotFoundException(MINISTERIO_NAO_ENCONTRADO);
        }
    }

}
