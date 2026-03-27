package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.UserUnavailability;
import br.com.louvor4.api.shared.dto.UserUnavailability.UserUnavailabilityResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserUnavailabilityMapper {

    @Mapping(target = "projectIds", expression = "java(entity.getProjects().stream()\n" +
            "                .map(br.com.louvor4.api.models.UserUnavailabilityProject::getProject)\n" +
            "                .map(br.com.louvor4.api.models.MusicProject::getId)\n" +
            "                .toList())")
    UserUnavailabilityResponse toDto(UserUnavailability entity);
}
