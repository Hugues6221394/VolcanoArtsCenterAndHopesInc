package com.volcanoartscenter.platform.shared.reference;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ReferenceCounterRepository extends JpaRepository<ReferenceCounter, ReferenceCounter.Pk> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ReferenceCounter c WHERE c.scope = :scope AND c.year = :year")
    Optional<ReferenceCounter> findForUpdate(@Param("scope") String scope,
                                             @Param("year") Integer year);
}
