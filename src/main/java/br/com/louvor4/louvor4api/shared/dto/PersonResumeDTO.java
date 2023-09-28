package br.com.louvor4.louvor4api.shared.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;


public record PersonResumeDTO(UUID id,  String firstName, String lastName, Date birthday) implements Serializable {
}

