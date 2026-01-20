package br.com.louvor4.api.controllers;


import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.services.MusicProjectService;
import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.MusicProject.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("music-project")
public class MusicProjectController {

    private final MusicProjectService musicProjectService;

    public MusicProjectController(MusicProjectService musicProjectService) {
        this.musicProjectService = musicProjectService;
    }


    @PostMapping("/create")
    public ResponseEntity<MusicProjectDetailDTO> create(@RequestBody @Valid MusicProjectCreateDTO createDto) {
        MusicProjectDetailDTO dto = musicProjectService.create(createDto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MusicProjectDetailDTO> findById(@PathVariable UUID id) {
        MusicProjectDetailDTO musicProjectDetailDTO = musicProjectService.getById(id);
        return ResponseEntity.ok(musicProjectDetailDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MusicProjectDetailDTO> update(@PathVariable UUID id, @RequestBody @Valid MusicProjectDTO updateDto) {
        MusicProjectDetailDTO musicProjectDetailDTO = musicProjectService.update(id,updateDto);
        return ResponseEntity.ok(musicProjectDetailDTO);
    }

    @PutMapping(value = "/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfileImage(
            @PathVariable UUID id,
            @RequestPart("profileImage") MultipartFile profileImage
    ) {
        String url = musicProjectService.updateImage(id, profileImage);
        return ResponseEntity.ok(url);
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<Void> addMember(@PathVariable UUID projectId, @RequestBody @Valid AddMemberDTO addDto) {
        musicProjectService.addMember(projectId, addDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<MemberDTO>> getMembers(@PathVariable UUID projectId) {
        List<MemberDTO> members = musicProjectService.getMembers(projectId);
        return ResponseEntity.ok(members);
    }


    @GetMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<MemberDTO> getMember(@PathVariable UUID projectId, @PathVariable UUID memberId) {
        MemberDTO members = musicProjectService.getMember(projectId, memberId);
        return ResponseEntity.ok(members);
    }

    @PutMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<MemberDTO> updateMember(@PathVariable UUID projectId, @PathVariable UUID memberId, @RequestBody UpdateMemberRequest request) {
        MemberDTO members = musicProjectService.updateMember(projectId, memberId,request);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{projectId}/events")
    public ResponseEntity<Void> createEvent(@PathVariable UUID projectId, @RequestBody @Valid CreateEventDto eventDto) {
        CreateEventDto createEvent = musicProjectService.createEvent(projectId, eventDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{projectId}/events")
    public ResponseEntity<List<EventDetailDto>> getEventsByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(musicProjectService.getEventsByProject(projectId));
    }

    @PostMapping("/{projectId}/skills")
    public ResponseEntity<Void> addProjectSkill(
            @PathVariable UUID projectId,
            @RequestBody @Valid ProjectSkillRequestDTO skillDto) {
        musicProjectService.addProjectSkill(projectId, skillDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{projectId}/skills")
    public ResponseEntity<List<ProjectSkillDTO>> getProjectSkills(@PathVariable UUID projectId) {
        List<ProjectSkillDTO> skills = musicProjectService.getProjectSkills(projectId);
        return ResponseEntity.ok(skills);
    }

    @PostMapping("/{projectId}/members/{memberId}/skills")
    public ResponseEntity<Void> assignSkillsToMember(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @RequestBody List<UUID> skillIds) {
        musicProjectService.assignSkillsToMember(projectId, memberId, skillIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/member-role")
    public ResponseEntity<ProjectMemberRole> getMemberRole(@PathVariable UUID projectId) {
        ProjectMemberRole memberRole = musicProjectService.getMemberRole(projectId);
        return ResponseEntity.ok(memberRole);
    }
}
