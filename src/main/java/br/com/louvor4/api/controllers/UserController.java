package br.com.louvor4.api.controllers;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.mapper.UserMapper;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.services.EventService;
import br.com.louvor4.api.services.MusicProjectService;
import br.com.louvor4.api.services.UserUnavailabilityService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.*;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.UserEventDetailDto;
import br.com.louvor4.api.shared.dto.MusicProject.MusicProjectDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import br.com.louvor4.api.shared.dto.UserUnavailability.CreateUserUnavailabilityRequest;
import br.com.louvor4.api.shared.dto.UserUnavailability.UserUnavailabilityResponse;
import br.com.louvor4.api.shared.dto.User.UserCreateDTO;
import br.com.louvor4.api.shared.dto.User.UserDetailDTO;
import br.com.louvor4.api.shared.dto.User.UserUpdateDTO;
import br.com.louvor4.api.shared.dto.notification.UnreadNotificationCountResponse;
import br.com.louvor4.api.shared.dto.notification.UserNotificationItemResponse;
import br.com.louvor4.api.shared.dto.notification.UserNotificationListResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static br.com.louvor4.api.shared.Messages.USER_CREATED_MESSAGE;
import static br.com.louvor4.api.shared.Messages.USER_CREATED_TITLE;

@RestController
@RequestMapping("users")
public class UserController {
    private final UserService userService;
    private final MusicProjectService musicProjectService;
    private final EventService eventService;
    private final UserMapper userMapper;
    private final UserNotificationService userNotificationService;
    private final UserUnavailabilityService userUnavailabilityService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, MusicProjectService musicProjectService, EventService eventService, UserMapper userMapper, UserNotificationService userNotificationService, UserUnavailabilityService userUnavailabilityService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.musicProjectService = musicProjectService;
        this.eventService = eventService;
        this.userMapper = userMapper;
        this.userNotificationService = userNotificationService;
        this.userUnavailabilityService = userUnavailabilityService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/detail")
    public ResponseEntity<UserDetailDTO> getUserDetail(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username);
       return ResponseEntity.status(HttpStatus.OK).body(userMapper.toDto(user));
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

    @GetMapping("/music-projects")
    public ResponseEntity<List<MusicProjectDTO>> getMusicProjects() {
        return ResponseEntity.ok(musicProjectService.getFromUser());
    }

    @GetMapping("/songs")
    public ResponseEntity<List<SongDTO>> getSongs() {
        List<SongDTO> dtoList = userService.getSongs();
        return ResponseEntity.ok(dtoList);
    }

    @GetMapping("/events")
    public ResponseEntity<List<UserEventDetailDto>> getEventsByUser() {
        return ResponseEntity.ok(eventService.getEventsByUser());
    }

    @GetMapping("/notifications")
    public ResponseEntity<UserNotificationListResponse> getNotifications(Pageable pageable) {
        User user = currentUserProvider.get();
        UserNotificationListResponse response = userNotificationService.listUnreadNotifications(user.getId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadNotificationCount() {
        User user = currentUserProvider.get();
        return ResponseEntity.ok(userNotificationService.countUnreadNotifications(user.getId()));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<UserNotificationItemResponse> markNotificationAsRead(@PathVariable java.util.UUID notificationId) {
        User user = currentUserProvider.get();
        return ResponseEntity.ok(userNotificationService.markAsRead(user.getId(), notificationId));
    }

    @PostMapping("/unavailabilities")
    public ResponseEntity<UserUnavailabilityResponse> createUnavailability(@RequestBody @Valid CreateUserUnavailabilityRequest request) {
        UserUnavailabilityResponse response = userUnavailabilityService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
