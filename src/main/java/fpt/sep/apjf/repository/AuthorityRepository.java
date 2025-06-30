package fpt.sep.apjf.repository;

import fpt.sep.apjf.entity.Authority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthorityRepository extends JpaRepository<Authority, Long> {

    /* tìm theo tên quyền: ROLE_USER, ROLE_STAFF … */
    Optional<Authority> findByAuthority(String authority);
}