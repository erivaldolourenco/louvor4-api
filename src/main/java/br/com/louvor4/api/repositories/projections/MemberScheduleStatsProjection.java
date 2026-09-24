package br.com.louvor4.api.repositories.projections;

import br.com.louvor4.api.enums.SkillIcon;

import java.util.UUID;

public interface MemberScheduleStatsProjection {
    UUID getMemberId();
    UUID getUserId();
    String getFirstName();
    String getLastName();
    String getProfileImage();
    UUID getSkillId();
    String getSkillName();
    SkillIcon getSkillIconKey();
    Integer getEventYear();
    Integer getEventMonth();
    Long getTotal();
}
