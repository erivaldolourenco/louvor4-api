package br.com.louvor4.api.models;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Período em que um membro fica com uma responsabilidade do projeto.
 * Cada troca de responsável é um novo registro, o que forma o histórico.
 */
@Entity
@Table(
        name = "project_responsibility_assignments",
        indexes = {
                @Index(name = "idx_resp_assignments_responsibility", columnList = "responsibility_id, start_date"),
                @Index(name = "idx_resp_assignments_member", columnList = "member_id, end_date")
        }
)
public class ProjectResponsibilityAssignment {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsibility_id", nullable = false, columnDefinition = "uuid")
    private ProjectResponsibility responsibility;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, columnDefinition = "uuid")
    private MusicProjectMember member;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    /** O período inclui o dia informado (início e fim inclusivos). */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /** Os dois períodos compartilham pelo menos um dia. */
    public boolean overlaps(LocalDate start, LocalDate end) {
        return !startDate.isAfter(end) && !endDate.isBefore(start);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProjectResponsibility getResponsibility() {
        return responsibility;
    }

    public void setResponsibility(ProjectResponsibility responsibility) {
        this.responsibility = responsibility;
    }

    public MusicProjectMember getMember() {
        return member;
    }

    public void setMember(MusicProjectMember member) {
        this.member = member;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
