package com.volcanoartscenter.platform.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicAndAdminRoutesTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicRoutesShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        mockMvc.perform(get("/art-store")).andExpect(status().isOk());
        mockMvc.perform(get("/experiences")).andExpect(status().isOk());
        mockMvc.perform(get("/conservation")).andExpect(status().isOk());
        mockMvc.perform(get("/talent")).andExpect(status().isOk());
        mockMvc.perform(get("/blog")).andExpect(status().isOk());
        mockMvc.perform(get("/contact")).andExpect(status().isOk());
    }

    @Test
    void adminDashboardShouldRequireAuth() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection());
    }
}
