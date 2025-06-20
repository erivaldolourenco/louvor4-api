package br.com.louvor4.api.shared.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(@NotBlank
                               @Email
                               String email) {
}
