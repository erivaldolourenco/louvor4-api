package br.com.louvor4.louvor4api.services;

import br.com.louvor4.louvor4api.converter.MinistryConverter;
import br.com.louvor4.louvor4api.dto.MinistryDTO;
import br.com.louvor4.louvor4api.dto.MinistryPersonDTO;
import br.com.louvor4.louvor4api.models.Member;
import br.com.louvor4.louvor4api.models.Ministry;
import br.com.louvor4.louvor4api.models.MinistryPermission;
import br.com.louvor4.louvor4api.models.Person;
import br.com.louvor4.louvor4api.repositories.MemberRepository;
import br.com.louvor4.louvor4api.repositories.MinistryPermissionRepository;
import br.com.louvor4.louvor4api.repositories.MinistryRepository;
import br.com.louvor4.louvor4api.repositories.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class MinistryService {
    private Logger logger = Logger.getLogger(MinistryService.class.getName());
    @Autowired
    MinistryRepository ministryRepository;

    @Autowired
    MinistryPermissionRepository permissionRepository;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PersonRepository personRepository;

    public MinistryDTO createMinistry(MinistryDTO ministryDto, String personEmail) {
        Ministry ministry = MinistryConverter.INSTANCE.toEntity(ministryDto);
        Member member = new Member();
        member.setPerson(personRepository.getPersonByEmail(personEmail));
        member.setMinistry(ministryRepository.save(ministry));
        memberRepository.save(member);
        return MinistryConverter.INSTANCE.toDto(ministry);
    }


    public void addMemberToMinistry(UUID ministeryId, UUID personId) {
        Ministry ministry = ministryRepository.findById(ministeryId).get();
        Person person = personRepository.findById(personId).get();

        Member member = new Member();
        member.setPerson(person);
        MinistryPermission leader = permissionRepository.getMinistryPermissionByDescription("LEADER");
        member.setMinistryPermissions(Arrays.asList(leader));
        memberRepository.save(member);

        ministry.getMembers().add(member);
        ministryRepository.save(ministry);
    }

    private List<Member> getMemberOfMinistry(UUID idMinistry) {
        Ministry ministry = ministryRepository.findById(idMinistry).get();
        return ministry.getMembers();
    }

    public List<MinistryDTO> getAllMinistries() {
        return MinistryConverter.INSTANCE.toDto(ministryRepository.findAll());
    }

    public void newRole(MinistryPersonDTO ministryPersonDTO) throws IllegalAccessException {
        Person person = personRepository.findById(ministryPersonDTO.idPerson()).get();

        //TODO verificar interceptação no endpoint da sefaz numeropessoa filter.
        if (isPersonMemberAndLeaderOfMinisterio(ministryPersonDTO.idPerson())) {
            //implemtação
        } else {
            throw new IllegalAccessException("Usuario nao tem para esse ministério!");
        }
    }

    public boolean isPersonMemberAndLeaderOfMinisterio(UUID userId) {
        return true;
    }

    public MinistryDTO getMinistry(UUID idMinistry) {
        return null;
    }
}
