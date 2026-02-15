package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.User;
import br.com.louvor4.api.shared.dto.User.UserDetailDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    UserDetailDTO toDto(User entity);
}
