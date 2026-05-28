package com.volcanoartscenter.platform.shared.fx;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRate, FxRate.Pk> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM FxRate r WHERE r.baseCurrency = :base AND r.quoteCurrency = :quote")
    Optional<FxRate> findForUpdate(@Param("base") String base, @Param("quote") String quote);
}
