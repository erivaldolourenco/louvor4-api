package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.MusicProject.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MusicProjectService {
    MusicProjectDetailDTO create(MusicProjectCreateDTO musicProjectCreateDTO);
    MusicProjectDetailDTO update(UUID id, MusicProjectDTO updateDTO);
    String updateImage(UUID projecId, MultipartFile profileImage);
    MusicProjectDetailDTO getMusicProjectById(UUID projectId);
    List<MusicProjectDTO> getMusicProjectFromUser();
    void addMember(UUID projectId, AddMemberDTO addDto);
    List<MemberDTO> getMembers(UUID id);
}
