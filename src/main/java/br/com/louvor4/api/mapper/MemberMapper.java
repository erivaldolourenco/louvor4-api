package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.ProjectSkill;
import br.com.louvor4.api.shared.dto.MusicProject.MemberDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface MemberMapper {

        @Mapping(target = "id", source = "id")
        @Mapping(target = "userId", source = "user.id")
        @Mapping(target = "firstName", source = "user.firstName")
        @Mapping(target = "lastName", source = "user.lastName")
        @Mapping(target = "email", source = "user.email")
        @Mapping(target = "profileImage", source = "user.profileImage")
        @Mapping(target = "projectRole", source = "projectRole")
        @Mapping(target = "status", source = "status")
        @Mapping(target = "skills", source = "projectSkills", qualifiedByName = "mapSkills")
        MemberDTO toDto(MusicProjectMember member);

    @Named("mapSkills")
    default Set<String> mapSkills(Set<ProjectSkill> projectSkills) {
        if (projectSkills == null) return new HashSet<>();
        return projectSkills.stream()
                .map(skill -> skill.getId().toString()) // Alterado de getName() para getId()
                .collect(Collectors.toSet());
    }
}
