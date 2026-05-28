package com.volcanoartscenter.platform.web;

import com.volcanoartscenter.platform.shared.model.AvailabilitySlot;
import com.volcanoartscenter.platform.shared.model.BlackoutDate;
import com.volcanoartscenter.platform.shared.model.Experience;
import com.volcanoartscenter.platform.shared.repository.AvailabilitySlotRepository;
import com.volcanoartscenter.platform.shared.repository.BlackoutDateRepository;
import com.volcanoartscenter.platform.shared.repository.ExperienceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAndRoleRoutesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExperienceRepository experienceRepository;

    @Autowired
    private AvailabilitySlotRepository availabilitySlotRepository;

    @Autowired
    private BlackoutDateRepository blackoutDateRepository;

    @Test
    void clientRoutesRequireRegisteredClientRole() throws Exception {
        mockMvc.perform(get("/client/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));

        mockMvc.perform(get("/client/dashboard")
                        .with(SecurityMockMvcRequestPostProcessors.user("ops@user").roles("OPS_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tourOperatorAndTalentDashboardsAreRoleProtected() throws Exception {
        mockMvc.perform(get("/tour-operators/portal")
                        .with(SecurityMockMvcRequestPostProcessors.user("client@user").roles("REGISTERED_CLIENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/talent/dashboard")
                        .with(SecurityMockMvcRequestPostProcessors.user("operator@user").roles("TOUR_OPERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void availabilitySlotsPageRendersRowsForOpsManagers() throws Exception {
        blackoutDateRepository.deleteAll();
        availabilitySlotRepository.deleteAll();
        experienceRepository.deleteAll();

        Experience experience = experienceRepository.save(Experience.builder()
                .title("Availability Test Experience")
                .slug("availability-test-experience")
                .shortDescription("Testing availability rendering")
                .description("Testing availability rendering")
                .pricePerPerson(new BigDecimal("45.00"))
                .experienceType(Experience.ExperienceType.CULTURAL)
                .bookingType(Experience.BookingType.DIRECT)
                .minGroupSize(1)
                .maxGroupSize(15)
                .active(true)
                .featured(false)
                .build());

        availabilitySlotRepository.save(AvailabilitySlot.builder()
                .experience(experience)
                .slotDate(LocalDate.of(2026, 5, 2))
                .status(AvailabilitySlot.SlotStatus.AVAILABLE)
                .maxCapacity(15)
                .bookedCount(0)
                .build());

        blackoutDateRepository.save(BlackoutDate.builder()
                .experience(experience)
                .dateValue(LocalDate.of(2026, 5, 3))
                .reason("Private hire")
                .build());

        mockMvc.perform(get("/admin/ops/availability-slots")
                        .with(SecurityMockMvcRequestPostProcessors.user("ops@user").roles("OPS_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Availability Test Experience")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Private hire")));
    }
}
