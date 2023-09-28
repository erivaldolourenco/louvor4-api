package br.com.louvor4.louvor4api.shared.projection;


import br.com.louvor4.louvor4api.shared.dto.PersonResumeDTO;


public interface MemberProjection {
    public Long getId();
    public PersonResumeDTO getPerson();
}
