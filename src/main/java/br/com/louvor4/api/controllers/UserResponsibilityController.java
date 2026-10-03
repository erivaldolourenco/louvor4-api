package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ResponsibilityAssignmentService;
import br.com.louvor4.api.shared.dto.Responsibility.MyResponsibilityDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Responsabilidades do usuário logado em todos os projetos (card da tela de Início). */
@RestController
@RequestMapping("users/responsibilities")
public class UserResponsibilityController {

    private final ResponsibilityAssignmentService assignmentService;

    public UserResponsibilityController(ResponsibilityAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public ResponseEntity<List<MyResponsibilityDTO>> listMine() {
        return ResponseEntity.ok(assignmentService.listMine());
    }
}
