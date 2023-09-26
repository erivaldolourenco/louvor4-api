package br.com.louvor4.louvor4api.repositories;

import br.com.louvor4.louvor4api.models.Member;
import br.com.louvor4.louvor4api.models.Ministry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MinistryRepository extends JpaRepository<Ministry, UUID> {
//    List<Ministry> findAllByMember(Member member);
}
