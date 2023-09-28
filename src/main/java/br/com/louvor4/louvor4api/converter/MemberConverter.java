package br.com.louvor4.louvor4api.converter;

import br.com.louvor4.louvor4api.models.Member;
import br.com.louvor4.louvor4api.models.Person;
import br.com.louvor4.louvor4api.shared.dto.MemberDTO;
import br.com.louvor4.louvor4api.shared.dto.PersonDTO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MemberConverter extends BaseConverter<Member, MemberDTO> {
    MemberConverter INSTANCE = Mappers.getMapper(MemberConverter.class);
}
