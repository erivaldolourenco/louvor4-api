package br.com.louvor4.api.services;

import br.com.louvor4.api.models.User;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import br.com.louvor4.api.shared.dto.User.UserCreateDTO;
import br.com.louvor4.api.shared.dto.User.UserDetailDTO;
import br.com.louvor4.api.shared.dto.User.UserUpdateDTO;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public interface UserService {
    User create(UserCreateDTO user);
    UserDetailDTO update(@Valid UserUpdateDTO updateDto);
    String updateImage(MultipartFile profileImage);
    User getUserById(UUID idUser);
    User findByUsername(String username);
    User findByEmail(String username);
    List<MusicProjectDTO> getMusicProjects();
    List<SongDTO> getSongs();


}
