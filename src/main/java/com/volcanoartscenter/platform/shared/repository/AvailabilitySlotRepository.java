package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.AvailabilitySlot;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlot, Long> {
    Optional<AvailabilitySlot> findByExperienceIdAndSlotDate(Long experienceId, LocalDate slotDate);

    @EntityGraph(attributePaths = {"experience"})
    List<AvailabilitySlot> findByExperienceIdAndSlotDateBetweenOrderBySlotDateAsc(Long experienceId, LocalDate from, LocalDate to);

    List<AvailabilitySlot> findByAssignedGuideEmailAndSlotDate(String assignedGuideEmail, LocalDate slotDate);

    @EntityGraph(attributePaths = {"experience"})
    List<AvailabilitySlot> findBySlotDateBetweenOrderBySlotDateAsc(LocalDate from, LocalDate to);

    @Override
    @EntityGraph(attributePaths = {"experience"})
    List<AvailabilitySlot> findAll();

    @Override
    @EntityGraph(attributePaths = {"experience"})
    Optional<AvailabilitySlot> findById(Long id);
}
