package br.com.louvor4.louvor4api.repositories;

import br.com.louvor4.louvor4api.models.Member;
import br.com.louvor4.louvor4api.models.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    Member getMemberByPerson(Person person);
}
