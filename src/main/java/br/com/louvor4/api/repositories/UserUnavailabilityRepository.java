package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.UserUnavailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserUnavailabilityRepository extends JpaRepository<UserUnavailability, UUID> {
    @Query("""
            select distinct uu
            from UserUnavailability uu
            left join fetch uu.projects uup
            left join fetch uup.project p
            where uu.user.id = :userId
              and uu.startDate <= :eventDate
              and uu.endDate >= :eventDate
            """)
    List<UserUnavailability> findActiveByUserIdAndEventDate(
            @Param("userId") UUID userId,
            @Param("eventDate") LocalDate eventDate
    );
}
