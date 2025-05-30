package br.com.louvor4.api.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserDTO {
    @NotBlank(message = "O primeiro nome é obrigatório")
    @Size(max = 100, message = "O primeiro nome deve ter no máximo 100 caracteres")
    private String firstName;

    @NotBlank(message = "O sobrenome é obrigatório")
    @Size(max = 100, message = "O sobrenome deve ter no máximo 100 caracteres")
    private String lastName;

    @NotBlank(message = "O nome de usuário é obrigatório")
    @Size(min = 5, max = 255, message = "O nome de usuário deve ter no  minimo 5  e no máximo 255 caracteres")
    private String username;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 6, max = 255, message = "A senha deve ter entre 6 e 255 caracteres")
    private String password;

    @Email(message = "E-mail inválido")
    @NotBlank(message = "O email é obrigatória")
    @Size(max = 255, message = "O e-mail deve ter no máximo 255 caracteres")
    private String email;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
