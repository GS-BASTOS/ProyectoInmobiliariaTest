package com.inmobiliaria.app.repo;

import com.inmobiliaria.app.domain.Client;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    @EntityGraph(attributePaths = {"phones"})
    List<Client> findAllByOrderByFullNameAsc();

    @EntityGraph(attributePaths = {"phones"})
    Optional<Client> findWithPhonesById(Long id);

    @EntityGraph(attributePaths = {"emails"})
    Optional<Client> findWithEmailsById(Long id);

    // ── NUEVO ──────────────────────────────────────────────
    @Query("""
        SELECT c FROM Client c
        WHERE (:q IS NULL OR :q = ''
               OR LOWER(c.fullName)    LIKE LOWER(CONCAT('%',:q,'%'))
               OR LOWER(c.companyName) LIKE LOWER(CONCAT('%',:q,'%')))
        ORDER BY c.fullName ASC
        """)
    List<Client> searchAll(@Param("q") String q);
    // ───────────────────────────────────────────────────────
}
