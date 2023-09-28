package br.com.louvor4.louvor4api.shared.dto;

import br.com.louvor4.louvor4api.models.Ministry;
import br.com.louvor4.louvor4api.models.Person;

import java.io.Serializable;

public record MemberDTO(Long id, PersonResumeDTO person, MinistryDTO ministry) implements Serializable {
}
