package br.com.louvor4.api.controllers;


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
        MusicProjectDetailDTO musicProjectDetailDTO = musicProjectService.getMusicProjectById(id);
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

    @PostMapping("/{projectId}/add-member")
    public ResponseEntity<Void> addMember(@PathVariable UUID projectId, @RequestBody @Valid AddMemberDTO addDto) {
        musicProjectService.addMember(projectId, addDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberDTO>> getMembers(@PathVariable UUID id) {
        List<MemberDTO> members = musicProjectService.getMembers(id);
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
}
