package com.volcanoartscenter.platform.web;

import com.volcanoartscenter.platform.shared.model.Role;
import com.volcanoartscenter.platform.shared.model.User;
import com.volcanoartscenter.platform.shared.repository.RoleRepository;
import com.volcanoartscenter.platform.shared.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndToEndJourneysTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void ensureOperatorUser() {
        Role role = roleRepository.findByName("TOUR_OPERATOR")
                .orElseGet(() -> roleRepository.save(Role.builder().name("TOUR_OPERATOR").description("test").build()));
        userRepository.findByEmail("operator@test.com").orElseGet(() ->
                userRepository.save(User.builder()
                        .email("operator@test.com")
                        .firstName("Tour")
                        .lastName("Operator")
                        .password(passwordEncoder.encode("Secret123!"))
                        .enabled(true)
                        .roles(Set.of(role))
                        .build()));
    }

    @Test
    void guestCanSubmitInquiryAndOperatorRequest() throws Exception {
        mockMvc.perform(post("/contact")
                        .with(csrf())
                        .param("name", "Guest User")
                        .param("email", "guest@test.com")
                        .param("subject", "general")
                        .param("message", "Hello team"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/tour-operators/request")
                        .with(csrf())
                        .param("companyName", "Safari Co")
                        .param("contactName", "Alex")
                        .param("contactEmail", "operator@test.com")
                        .param("invoiceRequired", "true"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void operatorPortalRequiresRoleAndAllowsRoleAccess() throws Exception {
        mockMvc.perform(get("/tour-operators/portal").param("email", "operator@test.com"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/tour-operators/portal")
                        .with(user("operator@test.com").roles("TOUR_OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void internalRoleRoutesProtected() throws Exception {
        mockMvc.perform(get("/admin/content/products"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/content/products")
                        .with(user("content@user").roles("CONTENT_MANAGER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/ops/bookings")
                        .with(user("ops@user").roles("OPS_MANAGER")))
                .andExpect(status().isOk());
    }
}
