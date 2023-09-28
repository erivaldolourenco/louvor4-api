package br.com.louvor4.louvor4api.shared.dto;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public record PersonDTO(UUID id, String email, String password, String firstName, String lastName, Date birthday) implements Serializable {
}

