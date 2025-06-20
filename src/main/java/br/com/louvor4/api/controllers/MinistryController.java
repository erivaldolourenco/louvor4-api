package br.com.louvor4.api.controllers;

import br.com.louvor4.api.models.Ministry;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.models.UserMinistry;
import br.com.louvor4.api.services.MinistryService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static br.com.louvor4.api.shared.Messages.*;

@RestController
@RequestMapping("ministries")
public class MinistryController {

    private final MinistryService ministryService;
    private final UserService userService;


    public MinistryController(MinistryService ministryService, UserService userService) {
        this.ministryService = ministryService;
        this.userService = userService;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Ministry>> create(@RequestBody @Valid MinistryCreateDTO ministryDTO) {

        Ministry ministry = ministryService.createMinistry(ministryDTO);

        ApiResponse<Ministry> response = ApiResponse.<Ministry>create()
                .withStatus(HttpStatus.CREATED.value())
                .withTitle(MINISTRY_CREATED_TITLE)
                .withMessage(MINISTRY_CREATED_MESSAGE)
                .withData(ministry);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ministryId}/detail")
    public ResponseEntity<MinistryDetailDTO> detail(@PathVariable UUID ministryId, Authentication authentication) {

        if (isUserMemberOfMinistry(authentication.getName(), ministryId)) {

            Ministry ministry = ministryService.getMinistryById(ministryId);

            List<UserDetailDTO> memberDTOs = ministry.getMembers().stream()
                    .map(UserMinistry::getUser)
                    .map(userMember -> new UserDetailDTO(
                            userMember.getId(),
                            userMember.getFirstName(),
                            userMember.getLastName(),
                            userMember.getEmail(),
                            userMember.getPhoneNumber(),
                            userMember.getProfileImage()
                            ))
                    .toList();

            MinistryDetailDTO ministryDTO = new MinistryDetailDTO(
                    ministryId,
                    ministry.getName(),
                    ministry.getDescription(),
                    ministry.getProfileImage(),
                    memberDTOs);
            return ResponseEntity.status(HttpStatus.OK).body(ministryDTO);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é pertence a este ministério.");
    }

    @PutMapping("/{ministryId}/update")
    public ResponseEntity<MinistryDetailDTO> update(
            @PathVariable UUID ministryId,
            @ModelAttribute("ministryUpdateDTO")
            @Valid MinistryUpdateDTO ministryUpdateDTO,
            BindingResult bindingResult,
            @RequestPart(value = "profileImage", required = false)
            MultipartFile profileImage,
            Authentication authentication
    ) {
        if (bindingResult.hasErrors()) {
            String errorMessages = bindingResult.getAllErrors().stream()
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessages);
        }

        if (isUserMemberAdminOfMinistry(authentication.getName(), ministryId)) {
            Ministry ministry = ministryService.updateMinistry(ministryId, ministryUpdateDTO, profileImage);

            List<UserDetailDTO> memberDTOs = ministry.getMembers().stream()
                    .map(UserMinistry::getUser)
                    .map(userMember -> new UserDetailDTO(
                            userMember.getId(),
                            userMember.getFirstName(),
                            userMember.getLastName(),
                            userMember.getEmail(),
                            userMember.getPhoneNumber(),
                            userMember.getProfileImage()
                    ))
                    .toList();

            MinistryDetailDTO ministryDTO = new MinistryDetailDTO(
                    ministryId,
                    ministry.getName(),
                    ministry.getDescription(),
                    ministry.getProfileImage(),
                    memberDTOs);

            return ResponseEntity.status(HttpStatus.CREATED).body(ministryDTO);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é ADMIN neste ministério.");
    }

    @PostMapping("/{ministryId}/add-member")
    public ResponseEntity<ApiResponse<String>> associateUsertoMinistry(
            @PathVariable UUID ministryId,
            @RequestBody @Valid AddMemberRequest addMemberRequest,
            Authentication authentication
    ) {
        if (isUserMemberAdminOfMinistry(authentication.getName(), ministryId)) {
            User userToassociate = userService.findByEmail(addMemberRequest.email());
            Ministry ministry = ministryService.getMinistryById(ministryId);
            ministryService.associateUsertoMinistry(userToassociate, ministry);

            ApiResponse<String> response = ApiResponse.<String>create()
                    .withStatus(HttpStatus.CREATED.value())
                    .withTitle(USER_ASSOCIATED_TITLE)
                    .withMessage(USER_ASSOCIATED_MESSAGE)
                    .withData("Usuário associado com sucesso");

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é ADMIN neste ministério.");
    }

    private boolean isUserMemberOfMinistry(String username, UUID ministryId) {
        User user = userService.findByUsername(username);
        return ministryService.isUserMemberOfMinistry(user.getId(), ministryId);
    }

    private boolean isUserMemberAdminOfMinistry(String username, UUID ministryId) {
        User user = userService.findByUsername(username);
        return ministryService.isUserMemberAdminOfMinistry(user.getId(), ministryId);
    }

}
