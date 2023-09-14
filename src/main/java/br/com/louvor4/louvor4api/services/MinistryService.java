package br.com.louvor4.louvor4api.services;

import br.com.louvor4.louvor4api.converter.MinistryConverter;
import br.com.louvor4.louvor4api.dto.MinistryDTO;
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

import java.util.ArrayList;
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

    public MinistryDTO create(MinistryDTO ministryDto, String personEmail) {
        Ministry ministry = MinistryConverter.INSTANCE.toEntity(ministryDto);
        Member member = memberRepository.getMemberByPerson(personRepository.getPersonByEmail(personEmail));
        if(member != null){
            ministry.getMember().add(member);
        }
        return MinistryConverter.INSTANCE.toDto(ministryRepository.save(ministry));
    }

    public void addMemberToMinistry(UUID ministeryId, UUID personId) {
        Ministry ministry = ministryRepository.findById(ministeryId).get();
        Person person = personRepository.findById(personId).get();

        Member member = new Member();
        member.setPerson(person);
        MinistryPermission leader = permissionRepository.getMinistryPermissionByDescription("LEADER");
        member.setMinistryPermissions(Arrays.asList(leader));
        memberRepository.save(member);

        ministry.getMember().add(member);
        ministryRepository.save(ministry);
    }

    private List<Member> getMemberOfMinistry(UUID idMinistry) {
        Ministry ministry = ministryRepository.findById(idMinistry).get();
        return ministry.getMember();
    }

    public List<MinistryDTO> getAll() {
        return MinistryConverter.INSTANCE.toDto(ministryRepository.findAll());
    }
}
