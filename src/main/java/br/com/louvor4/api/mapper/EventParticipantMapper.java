package br.com.louvor4.api.mapper;

import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.shared.dto.eventOverview.MonthEventParticipantItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventParticipantMapper {

    @Mapping(target = "memberId", source = "member.id")
    @Mapping(target = "firstName", source = "member.user.firstName")
    @Mapping(target = "lastName", source = "member.user.lastName")
    @Mapping(target = "profileImage", source = "member.user.profileImage")
    MonthEventParticipantItem toMonthOverviewParticipant(EventParticipant entity);

    List<MonthEventParticipantItem> toMonthOverviewParticipantList(List<EventParticipant> entities);
}
