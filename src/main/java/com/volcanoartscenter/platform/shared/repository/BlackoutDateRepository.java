package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.BlackoutDate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BlackoutDateRepository extends JpaRepository<BlackoutDate, Long> {
    Optional<BlackoutDate> findByExperienceIdAndDateValue(Long experienceId, LocalDate dateValue);

    @EntityGraph(attributePaths = {"experience"})
    List<BlackoutDate> findByExperienceIdOrderByDateValueAsc(Long experienceId);

    @EntityGraph(attributePaths = {"experience"})
    List<BlackoutDate> findAllByOrderByDateValueAsc();

    @Override
    @EntityGraph(attributePaths = {"experience"})
    Optional<BlackoutDate> findById(Long id);
}
