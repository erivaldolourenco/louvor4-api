package br.com.louvor4.louvor4api.converter;

import br.com.louvor4.louvor4api.shared.dto.PersonDTO;
import br.com.louvor4.louvor4api.models.Person;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PersonConverter extends BaseConverter<Person, PersonDTO> {
    PersonConverter INSTANCE = Mappers.getMapper(PersonConverter.class);
}
