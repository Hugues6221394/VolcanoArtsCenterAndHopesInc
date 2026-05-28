package com.volcanoartscenter.platform.shared.service;

import com.volcanoartscenter.platform.shared.model.AvailabilitySlot;
import com.volcanoartscenter.platform.shared.model.BlackoutDate;
import com.volcanoartscenter.platform.shared.model.Experience;
import com.volcanoartscenter.platform.shared.repository.AvailabilitySlotRepository;
import com.volcanoartscenter.platform.shared.repository.BlackoutDateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final BlackoutDateRepository blackoutDateRepository;

    public AvailabilitySlot getOrCreateSlot(Experience experience, LocalDate date) {
        if (blackoutDateRepository.findByExperienceIdAndDateValue(experience.getId(), date).isPresent()) {
            return availabilitySlotRepository.findByExperienceIdAndSlotDate(experience.getId(), date)
                    .orElseGet(() -> availabilitySlotRepository.save(
                            AvailabilitySlot.builder()
                                    .experience(experience)
                                    .slotDate(date)
                                    .status(AvailabilitySlot.SlotStatus.FULLY_BOOKED)
                                    .maxCapacity(0)
                                    .bookedCount(0)
                                    .build()
                    ));
        }
        return availabilitySlotRepository.findByExperienceIdAndSlotDate(experience.getId(), date)
                .orElseGet(() -> availabilitySlotRepository.save(
                        AvailabilitySlot.builder()
                                .experience(experience)
                                .slotDate(date)
                                .status(experience.getBookingType() == Experience.BookingType.INQUIRY
                                        ? AvailabilitySlot.SlotStatus.REQUEST_ONLY
                                        : AvailabilitySlot.SlotStatus.AVAILABLE)
                                .maxCapacity(experience.getMaxGroupSize() == null ? 15 : experience.getMaxGroupSize())
                                .bookedCount(0)
                                .build()
                ));
    }

    public List<AvailabilitySlot> next30Days(Experience experience) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(30);
        return availabilitySlotRepository.findByExperienceIdAndSlotDateBetweenOrderBySlotDateAsc(experience.getId(), from, to);
    }

    public void applyBookingToSlot(Experience experience, LocalDate date, int groupSize) {
        AvailabilitySlot slot = getOrCreateSlot(experience, date);
        if (slot.getStatus() == AvailabilitySlot.SlotStatus.FULLY_BOOKED) {
            throw new IllegalStateException("Selected date is not available");
        }
        int capacity = Math.max(1, slot.getMaxCapacity());
        int requested = Math.max(1, groupSize);
        int currentBooked = Math.max(0, slot.getBookedCount());
        if (currentBooked + requested > capacity) {
            throw new IllegalStateException("Selected date does not have enough remaining capacity");
        }
        int updatedBooked = currentBooked + requested;
        slot.setBookedCount(updatedBooked);
        recalculateStatus(slot);
        availabilitySlotRepository.save(slot);
    }

    public void releaseBookingFromSlot(Experience experience, LocalDate date, int groupSize) {
        if (experience == null || date == null) {
            return;
        }
        AvailabilitySlot slot = availabilitySlotRepository.findByExperienceIdAndSlotDate(experience.getId(), date)
                .orElse(null);
        if (slot == null) {
            return;
        }

        int updatedBooked = Math.max(0, Math.max(0, slot.getBookedCount()) - Math.max(1, groupSize));
        slot.setBookedCount(updatedBooked);

        boolean isBlackout = blackoutDateRepository.findByExperienceIdAndDateValue(experience.getId(), date).isPresent();
        if (isBlackout) {
            slot.setStatus(AvailabilitySlot.SlotStatus.FULLY_BOOKED);
            slot.setMaxCapacity(0);
        } else {
            if (slot.getMaxCapacity() == null || slot.getMaxCapacity() < 1) {
                slot.setMaxCapacity(experience.getMaxGroupSize() == null ? 15 : experience.getMaxGroupSize());
            }
            if (experience.getBookingType() == Experience.BookingType.INQUIRY) {
                slot.setStatus(AvailabilitySlot.SlotStatus.REQUEST_ONLY);
            } else {
                recalculateStatus(slot);
            }
        }
        availabilitySlotRepository.save(slot);
    }

    public void recalculateStatus(AvailabilitySlot slot) {
        if (slot.getStatus() == AvailabilitySlot.SlotStatus.REQUEST_ONLY) {
            return;
        }
        int capacity = Math.max(1, slot.getMaxCapacity());
        if (slot.getBookedCount() >= capacity) {
            slot.setStatus(AvailabilitySlot.SlotStatus.FULLY_BOOKED);
        } else if (slot.getBookedCount() >= Math.ceil(capacity * 0.7)) {
            slot.setStatus(AvailabilitySlot.SlotStatus.LIMITED);
        } else {
            slot.setStatus(AvailabilitySlot.SlotStatus.AVAILABLE);
        }
    }

    public void setStatus(Long slotId, AvailabilitySlot.SlotStatus status) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId).orElseThrow();
        slot.setStatus(status);
        availabilitySlotRepository.save(slot);
    }

    public void generateRecurringSlots(Experience experience, LocalDate from, LocalDate to, int defaultCapacity) {
        LocalDate cursor = from;
        while (!cursor.isAfter(to)) {
            getOrCreateSlot(experience, cursor);
            AvailabilitySlot slot = availabilitySlotRepository.findByExperienceIdAndSlotDate(experience.getId(), cursor).orElseThrow();
            boolean isBlackout = blackoutDateRepository.findByExperienceIdAndDateValue(experience.getId(), cursor).isPresent();
            if (isBlackout) {
                slot.setStatus(AvailabilitySlot.SlotStatus.FULLY_BOOKED);
                slot.setMaxCapacity(0);
                availabilitySlotRepository.save(slot);
            } else if (slot.getBookedCount() == 0) {
                slot.setMaxCapacity(Math.max(1, defaultCapacity));
                if (experience.getBookingType() == Experience.BookingType.INQUIRY) {
                    slot.setStatus(AvailabilitySlot.SlotStatus.REQUEST_ONLY);
                } else {
                    recalculateStatus(slot);
                }
                availabilitySlotRepository.save(slot);
            } else if (slot.getMaxCapacity() == null || slot.getMaxCapacity() < 1) {
                slot.setMaxCapacity(Math.max(1, defaultCapacity));
                availabilitySlotRepository.save(slot);
            }
            cursor = cursor.plusDays(1);
        }
    }

    public void addBlackoutDate(Experience experience, LocalDate date, String reason) {
        if (blackoutDateRepository.findByExperienceIdAndDateValue(experience.getId(), date).isPresent()) {
            return;
        }
        blackoutDateRepository.save(BlackoutDate.builder()
                .experience(experience)
                .dateValue(date)
                .reason(reason)
                .build());
        AvailabilitySlot slot = getOrCreateSlot(experience, date);
        slot.setStatus(AvailabilitySlot.SlotStatus.FULLY_BOOKED);
        slot.setMaxCapacity(0);
        availabilitySlotRepository.save(slot);
    }

    public void assignGuide(Long slotId, String guideEmail, String guideName) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId).orElseThrow();
        if (guideEmail == null || guideEmail.isBlank()) {
            slot.setAssignedGuideEmail(null);
            slot.setAssignedGuideName(null);
            availabilitySlotRepository.save(slot);
            return;
        }
        boolean hasConflict = availabilitySlotRepository.findByAssignedGuideEmailAndSlotDate(guideEmail, slot.getSlotDate())
                .stream()
                .anyMatch(existing -> !existing.getId().equals(slotId));
        if (hasConflict) {
            throw new IllegalStateException("Guide already assigned on the selected date");
        }
        slot.setAssignedGuideEmail(guideEmail.trim().toLowerCase());
        slot.setAssignedGuideName(guideName == null || guideName.isBlank() ? guideEmail : guideName);
        availabilitySlotRepository.save(slot);
    }
}
