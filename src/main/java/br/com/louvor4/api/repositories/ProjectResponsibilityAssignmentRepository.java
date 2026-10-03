package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.models.ProjectResponsibilityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectResponsibilityAssignmentRepository extends JpaRepository<ProjectResponsibilityAssignment, UUID> {

    @Query("""
            SELECT a FROM ProjectResponsibilityAssignment a
            JOIN FETCH a.member m
            JOIN FETCH m.user
            WHERE a.responsibility.id = :responsibilityId
            ORDER BY a.startDate DESC
            """)
    List<ProjectResponsibilityAssignment> findHistoryByResponsibilityId(@Param("responsibilityId") UUID responsibilityId);

    /** Todos os períodos das responsabilidades de um projeto (para montar atual/próximo/último numa consulta só). */
    @Query("""
            SELECT a FROM ProjectResponsibilityAssignment a
            JOIN FETCH a.member m
            JOIN FETCH m.user
            WHERE a.responsibility.musicProject.id = :projectId
            """)
    List<ProjectResponsibilityAssignment> findAllByProjectId(@Param("projectId") UUID projectId);

    @Query("""
            SELECT a FROM ProjectResponsibilityAssignment a
            WHERE a.id = :assignmentId
              AND a.responsibility.id = :responsibilityId
              AND a.responsibility.musicProject.id = :projectId
            """)
    Optional<ProjectResponsibilityAssignment> findInProject(@Param("assignmentId") UUID assignmentId,
                                                            @Param("responsibilityId") UUID responsibilityId,
                                                            @Param("projectId") UUID projectId);

    /** Períodos vigentes ou agendados do usuário, só em projetos onde ele ainda é membro ativo. */
    @Query("""
            SELECT a FROM ProjectResponsibilityAssignment a
            JOIN FETCH a.responsibility r
            JOIN FETCH r.musicProject p
            JOIN a.member m
            WHERE m.user.id = :userId
              AND m.status = :activeStatus
              AND a.endDate >= :today
            ORDER BY a.startDate ASC
            """)
    List<ProjectResponsibilityAssignment> findUpcomingByUserId(@Param("userId") UUID userId,
                                                               @Param("activeStatus") ProjectMemberStatus activeStatus,
                                                               @Param("today") LocalDate today);

    @Modifying
    @Query("DELETE FROM ProjectResponsibilityAssignment a WHERE a.responsibility.id = :responsibilityId")
    void deleteByResponsibilityId(@Param("responsibilityId") UUID responsibilityId);

    /** Remove períodos que ainda não começaram (membro saiu do projeto). */
    @Modifying
    @Query("DELETE FROM ProjectResponsibilityAssignment a WHERE a.member.id = :memberId AND a.startDate > :today")
    void deleteNotStartedByMemberId(@Param("memberId") UUID memberId, @Param("today") LocalDate today);

    /** Remove períodos que ainda não começaram de todos os vínculos do usuário (conta excluída). */
    @Modifying
    @Query("DELETE FROM ProjectResponsibilityAssignment a WHERE a.member.user.id = :userId AND a.startDate > :today")
    void deleteNotStartedByUserId(@Param("userId") UUID userId, @Param("today") LocalDate today);
}
