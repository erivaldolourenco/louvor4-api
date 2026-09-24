package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.AccountDeletionService;
import br.com.louvor4.api.shared.dto.User.RequestAccountDeletionDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("account-deletion-requests")
public class AccountDeletionController {

    private final AccountDeletionService accountDeletionService;

    public AccountDeletionController(AccountDeletionService accountDeletionService) {
        this.accountDeletionService = accountDeletionService;
    }

    @PostMapping
    public ResponseEntity<String> request(@RequestBody @Valid RequestAccountDeletionDTO request) {
        accountDeletionService.requestDeletion(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Se este e-mail estiver cadastrado, você receberá um link para confirmar a exclusão da conta.");
    }

    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestParam("token") String token) {
        accountDeletionService.confirmDeletion(token);
        return ResponseEntity.ok("Sua conta e seus dados pessoais foram excluídos com sucesso.");
    }
}
