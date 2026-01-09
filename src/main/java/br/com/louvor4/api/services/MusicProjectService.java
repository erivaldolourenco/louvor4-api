package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.MusicProject.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MusicProjectService {
    MusicProjectDetailDTO create(MusicProjectCreateDTO musicProjectCreateDTO);
    MusicProjectDetailDTO update(UUID id, MusicProjectDTO updateDTO);
    String updateImage(UUID projecId, MultipartFile profileImage);
    MusicProjectDetailDTO getById(UUID projectId);
    List<MusicProjectDTO> getFromUser();

    void addMember(UUID projectId, AddMemberDTO addDto);
    List<MemberDTO> getMembers(UUID projectId);

    CreateEventDto createEvent(UUID projectId,CreateEventDto eventDto);
    List<EventDetailDto> getEventsByProject(UUID projectId);

    void assignSkillsToMember(UUID projectId, UUID memberId, List<UUID> skillIds);
    void addProjectSkill(UUID projectId, ProjectSkillRequestDTO skillDto);

    List<ProjectSkillDTO> getProjectSkills(UUID projectId);

    MemberDTO getMember(UUID projectId, UUID memberId);

    MemberDTO updateMember(UUID projectId, UUID memberId, UpdateMemberRequest request);
}
