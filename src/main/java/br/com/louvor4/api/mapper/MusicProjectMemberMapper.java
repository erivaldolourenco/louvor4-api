package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.shared.dto.MusicProject.MemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MusicProjectMemberMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "profileImage", source = "user.profileImage")
    @Mapping(target = "projectRole", source = "projectRole")
    MemberDTO toDto(MusicProjectMember entity);

    List<MemberDTO> toDtoList(List<MusicProjectMember> entities);

}
