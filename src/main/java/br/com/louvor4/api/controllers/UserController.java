package br.com.louvor4.api.controllers;

import br.com.louvor4.api.shared.dto.AuthenticationDTO;
import br.com.louvor4.api.shared.dto.CreateUserDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("users")
public class UserController {


    @PostMapping("/criar")
    public ResponseEntity criar(@RequestBody @Valid CreateUserDTO userDTO){
        System.out.println(userDTO);
        return ResponseEntity.ok().build();
    }
}
