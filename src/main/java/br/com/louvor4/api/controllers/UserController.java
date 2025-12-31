package br.com.louvor4.api.controllers;

import br.com.louvor4.api.models.Ministry;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.services.MinistryService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.*;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDTO;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDetailDTO;
import br.com.louvor4.api.shared.dto.User.UserCreateDTO;
import br.com.louvor4.api.shared.dto.User.UserDetailDTO;
import br.com.louvor4.api.shared.dto.User.UserMinistriesDTO;
import br.com.louvor4.api.shared.dto.User.UserUpdateDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static br.com.louvor4.api.shared.Messages.USER_CREATED_MESSAGE;
import static br.com.louvor4.api.shared.Messages.USER_CREATED_TITLE;

@RestController
@RequestMapping("users")
public class UserController {
    private final UserService userService;
    private final MinistryService ministryService;

    public UserController(UserService userService, MinistryService ministryService) {
        this.userService = userService;
        this.ministryService = ministryService;
    }

    @GetMapping("/detail")
    public ResponseEntity<UserDetailDTO> getUserDetail(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        UserDetailDTO userDetailDTO = new UserDetailDTO(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getProfileImage());
       return ResponseEntity.status(HttpStatus.OK).body(userDetailDTO);
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<User>> create(@RequestBody @Valid UserCreateDTO userDTO) {

        User user = userService.create(userDTO);

        ApiResponse<User> response = ApiResponse.<User>create()
                .withStatus(HttpStatus.CREATED.value())
                .withTitle(USER_CREATED_TITLE)
                .withMessage(USER_CREATED_MESSAGE)
                .withData(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/update")
    public ResponseEntity<UserDetailDTO> update(@RequestBody @Valid UserUpdateDTO updateDto) {
        UserDetailDTO userDetailDTO = userService.update(updateDto);
        return ResponseEntity.ok(userDetailDTO);
    }

    @PutMapping(value = "/update/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfileImage(
            @RequestPart("profileImage") MultipartFile profileImage
    ) {
        String url = userService.updateImage(profileImage);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/ministries")
    public ResponseEntity<List<UserMinistriesDTO>> getMyMinistries(Authentication authentication) {
        String username = authentication.getName(); // ou extrair de token
        User user = userService.findByUsername(username);
        List<Ministry> ministries = ministryService.getMinistriesByUser(user.getId());
        // Converter List<Ministry> para List<UserMinistriesDTO>
        List<UserMinistriesDTO> ministriesDto = ministries.stream()
                .map(ministry -> new UserMinistriesDTO(
                                ministry.getId(),
                                ministry.getName(),
                                ministry.getDescription(),
                                ministry.getProfileImage(),
                                (long) ministry.getMembers().size()
                        )
                )
                .toList();

        return ResponseEntity.ok(ministriesDto);
    }


    @GetMapping("/music-projects")
    public ResponseEntity<List<MusicProjectDTO>> getMusicProjects(Authentication authentication) {
        return ResponseEntity.ok(userService.getMusicProjects());
    }
}
