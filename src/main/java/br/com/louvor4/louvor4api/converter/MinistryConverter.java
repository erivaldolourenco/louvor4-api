package br.com.louvor4.louvor4api.converter;

import br.com.louvor4.louvor4api.dto.MinistryDTO;
import br.com.louvor4.louvor4api.models.Ministry;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface MinistryConverter extends BaseConverter<Ministry, MinistryDTO> {
    MinistryConverter INSTANCE = Mappers.getMapper(MinistryConverter.class);
}
