package br.com.louvor4.api.controllers;

import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.models.Ministry;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.services.MinistryService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.ApiResponse;
import br.com.louvor4.api.shared.dto.MinistryCreateDTO;
import br.com.louvor4.api.shared.dto.MinistryDetailDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static br.com.louvor4.api.shared.Messages.USER_CREATED_MESSAGE;
import static br.com.louvor4.api.shared.Messages.USER_CREATED_TITLE;

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
    public ResponseEntity<ApiResponse<Ministry>> create(@RequestBody @Valid MinistryCreateDTO ministryDTO){

        Ministry ministry =  ministryService.createMinistry(ministryDTO);

        ApiResponse<Ministry> response = ApiResponse.<Ministry>create()
                .withStatus(HttpStatus.CREATED.value())
                .withTitle(USER_CREATED_TITLE)
                .withMessage(USER_CREATED_MESSAGE)
                .withData(ministry);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{ministryId}/detail")
    public ResponseEntity<MinistryDetailDTO> detail(@PathVariable UUID ministryId, Authentication authentication){
        String username = authentication.getName();
        User user = userService.findByUsername(username);
        if(ministryService.isUserMemberOfMinistry(user.getId(), ministryId)){
            MinistryDetailDTO ministryDTO = new MinistryDetailDTO(ministryId,"23424","23424",null);
            return ResponseEntity.status(HttpStatus.OK).body(ministryDTO);
        }
         throw new NotFoundException("NAO PARTENCE A ESSA MINISTERIO");

    }

}
