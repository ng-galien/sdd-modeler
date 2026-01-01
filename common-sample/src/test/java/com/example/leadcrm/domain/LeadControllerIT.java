package com.example.leadcrm.domain;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for the generated LeadController using MockMvc.
 *
 * <p>Tests the REST API endpoints and verifies correct interaction with the database.
 *
 * <p><strong>Configuration:</strong> Uses environment variables or Gradle properties for PostgreSQL connection:
 * <ul>
 *   <li>POSTGRES_HOST (default: localhost)</li>
 *   <li>POSTGRES_PORT (default: 5432)</li>
 *   <li>POSTGRES_DB (default: sdd_test)</li>
 *   <li>POSTGRES_USER (default: test)</li>
 *   <li>POSTGRES_PASSWORD (default: test)</li>
 * </ul>
 *
 * <p><strong>Note:</strong> These tests require PostgreSQL to be available; they fail fast otherwise.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@ExtendWith(SpringExtension.class)
class LeadControllerIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("sdd_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&currentSchema=public,public_states");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.liquibase.drop-first", () -> true);
        registry.add("spring.liquibase.contexts", () -> "test");
        registry.add("spring.liquibase.default-schema", () -> "public");
    }

    @BeforeEach
    void cleanupData() {
        jdbcTemplate.execute("TRUNCATE TABLE public_states.leads_converted CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE public_states.leads_qualified CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE public_states.leads_contacted CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE public_states.leads_new CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE public.leads CASCADE");
    }

    @Test
    void shouldGetLeadById() throws Exception {
        // Given - A lead exists in the new state
        UUID leadId = createLeadDirectly("Jane Smith", "jane@example.com", "555-5678", "referral");

        // When - Fetching the lead in new state by ID
        mockMvc.perform(get("/api/leads/new/{id}", leadId.toString()))
                // Then - Should return the lead
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leadId.toString()));
    }

    @Test
    void shouldListAllLeads() throws Exception {
        // Given - Multiple leads exist
        createLeadDirectly("Alice", "alice@example.com", "555-0001", "web");
        createLeadDirectly("Bob", "bob@example.com", "555-0002", "phone");

        // When - Listing all leads
        mockMvc.perform(get("/api/leads"))
                // Then - Should return all leads with state info
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].stateType").exists());
    }

    @Test
    void shouldReturn404ForNonExistentLead() throws Exception {
        // Given - A random UUID that doesn't exist
        UUID nonExistentId = UUID.randomUUID();

        // When - Attempting to fetch non-existent lead
        mockMvc.perform(get("/api/leads/new/{id}", nonExistentId.toString()))
                // Then - Should return 404
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetLeadInContactedState() throws Exception {
        // Given - A lead that has transitioned to contacted state
        UUID leadId = createLeadDirectly("Contacted Lead", "contacted@example.com", "555-6666", "referral");
        transitionToContactedDirectly(leadId);

        // When/Then - Should be able to get lead from contacted state
        mockMvc.perform(get("/api/leads/contacted/{id}", leadId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leadId.toString()));
    }

    @Test
    void shouldReturn404WhenLeadNotInRequestedState() throws Exception {
        // Given - A lead in new state only
        UUID leadId = createLeadDirectly("New Only", "newonly@example.com", "555-5555", "web");

        // When/Then - Should get 404 when trying to get from contacted state (hasn't transitioned)
        mockMvc.perform(get("/api/leads/contacted/{id}", leadId.toString())).andExpect(status().isNotFound());
    }

    // Helper methods for direct database setup

    private UUID createLeadDirectly(String name, String email, String phone, String source) throws Exception {
        UUID leadId = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO public.leads (id) VALUES (?)", leadId);
        jdbcTemplate.update(
                "INSERT INTO public_states.leads_new (lead_id, name, email, phone, source) VALUES (?,?,?,?,?)",
                leadId,
                name,
                email,
                phone,
                source);

        return leadId;
    }

    private void transitionToContactedDirectly(UUID leadId) throws Exception {
        Long previousNewId = jdbcTemplate.queryForObject(
                "SELECT id FROM public_states.leads_new WHERE lead_id = ?", Long.class, leadId);
        jdbcTemplate.update(
                "INSERT INTO public_states.leads_contacted (lead_id, previous_new_id, contacted_at, contacted_by, notes) "
                        + "VALUES (?,?, NOW(), 'Test Agent', 'Test notes')",
                leadId,
                previousNewId);
    }
}
