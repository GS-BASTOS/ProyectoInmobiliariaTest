package com.inmobiliaria.app.repo;

import com.inmobiliaria.app.domain.Visit;
import com.inmobiliaria.app.domain.VisitStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    @EntityGraph(attributePaths = {"client", "property"})
    @Query("SELECT v FROM Visit v WHERE v.status = :status ORDER BY DATE(v.visitAt) DESC, v.visitAt ASC")
    List<Visit> findByStatusOrderByDateDescTimeAsc(@Param("status") VisitStatus status);

    @EntityGraph(attributePaths = {"client", "property"})
    List<Visit> findByClient_IdOrderByVisitAtDescIdDesc(Long clientId);

    @EntityGraph(attributePaths = {"client", "property"})
    List<Visit> findByStatusAndVisitAtBetweenOrderByVisitAtAsc(
            VisitStatus status, LocalDateTime from, LocalDateTime to);

    // Usado para borrar visitas antes de eliminar un inmueble
    List<Visit> findByProperty_Id(Long propertyId);
}
