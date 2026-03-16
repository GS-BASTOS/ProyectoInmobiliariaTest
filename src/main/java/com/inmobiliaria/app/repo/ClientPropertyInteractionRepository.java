package com.inmobiliaria.app.repo;

import com.inmobiliaria.app.domain.ClientPropertyInteraction;
import com.inmobiliaria.app.domain.ContactChannel;
import com.inmobiliaria.app.domain.InterestStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ClientPropertyInteractionRepository
        extends JpaRepository<ClientPropertyInteraction, Long> {

    @Query("""
        select i
        from ClientPropertyInteraction i
        join fetch i.client c
        join fetch i.property p
        order by i.contactDate desc, i.id desc
    """)
    List<ClientPropertyInteraction> findAllWithClientAndPropertyOrderByContactDateDesc();

    @Query("""
        select i
        from ClientPropertyInteraction i
        join fetch i.property p
        where i.client.id = :clientId
        order by i.contactDate desc, i.id desc
    """)
    List<ClientPropertyInteraction> findByClientIdWithPropertyOrderByContactDateDesc(
            @Param("clientId") Long clientId);

    @Query("""
        select i
        from ClientPropertyInteraction i
        join fetch i.client c
        join fetch i.property p
        where i.status = :status
        order by i.contactDate desc, i.id desc
    """)
    List<ClientPropertyInteraction> findByStatusWithClientAndPropertyOrderByContactDateDesc(
            @Param("status") InterestStatus status);

    @Query("""
        select max(i2.id)
        from ClientPropertyInteraction i2
        where i2.contactDate = (
            select max(i3.contactDate)
            from ClientPropertyInteraction i3
            where i3.client.id = i2.client.id
        )
        group by i2.client.id
    """)
    List<Long> findLastInteractionIdsPerClientByDateExact();

    @Query("""
        select i
        from ClientPropertyInteraction i
        join fetch i.client c
        join fetch i.property p
        where i.id in :ids
    """)
    List<ClientPropertyInteraction> findByIdInWithClientAndProperty(
            @Param("ids") List<Long> ids);

    @Query("""
        select i
        from ClientPropertyInteraction i
        join fetch i.client c
        join fetch i.property p
        where (:status is null or i.status = :status)
          and (:channel is null or i.channel = :channel)
          and (
               :q is null or :q = '' or
               lower(c.fullName) like lower(concat('%', :q, '%')) or
               lower(p.propertyCode) like lower(concat('%', :q, '%')) or
               lower(p.propertyType) like lower(concat('%', :q, '%')) or
               lower(p.address) like lower(concat('%', :q, '%')) or
               lower(p.municipality) like lower(concat('%', :q, '%')) or
               lower(i.comments) like lower(concat('%', :q, '%')) or
               lower(i.solviaCode) like lower(concat('%', :q, '%'))
          )
        order by i.contactDate desc, i.id desc
    """)
    List<ClientPropertyInteraction> searchWithFilters(
            @Param("status") InterestStatus status,
            @Param("channel") ContactChannel channel,
            @Param("q") String q
    );

    @Query("""
        select i
        from ClientPropertyInteraction i
        join fetch i.client c
        join fetch i.property p
        where (:status is null or i.status = :status)
          and (:channel is null or i.channel = :channel)
          and (:from is null or i.contactDate >= :from)
          and (:to is null or i.contactDate <= :to)
          and (
               :q is null or :q = '' or
               lower(c.fullName) like lower(concat('%', :q, '%')) or
               lower(p.propertyCode) like lower(concat('%', :q, '%')) or
               lower(p.propertyType) like lower(concat('%', :q, '%')) or
               lower(p.address) like lower(concat('%', :q, '%')) or
               lower(p.municipality) like lower(concat('%', :q, '%')) or
               lower(i.comments) like lower(concat('%', :q, '%')) or
               lower(i.solviaCode) like lower(concat('%', :q, '%')) or
               lower(coalesce(c.companyName, '')) like lower(concat('%', :q, '%'))
          )
        order by i.contactDate desc, i.id desc
    """)
    List<ClientPropertyInteraction> searchWithFilters(
            @Param("status") InterestStatus status,
            @Param("channel") ContactChannel channel,
            @Param("q") String q,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    long countByPropertyId(Long propertyId);

    // Usado para borrar interacciones antes de eliminar un inmueble
    List<ClientPropertyInteraction> findByPropertyId(Long propertyId);

    List<ClientPropertyInteraction> findByProperty_IdOrderByContactDateDesc(Long propertyId);

    @Query("""
        select p.propertyCode, count(i) as cnt
        from ClientPropertyInteraction i
        join i.property p
        group by p.id, p.propertyCode
        order by cnt desc
    """)
    List<Object[]> findTopPropertiesByInteractionCount(Pageable pageable);
}
