package com.example.leadcrm.domain;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import io.statemodeler.dsl.YamlModelLoader;
import io.statemodeler.sql.DdlGenerators;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

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
class LeadControllerIT {

    private static final String POSTGRES_HOST = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
    private static final String POSTGRES_PORT = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
    private static final String POSTGRES_DB = System.getenv().getOrDefault("POSTGRES_DB", "sdd_test");
    private static final String POSTGRES_USER = System.getenv().getOrDefault("POSTGRES_USER", "test");
    private static final String POSTGRES_PASSWORD = System.getenv().getOrDefault("POSTGRES_PASSWORD", "test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Include currentSchema parameter so Spring Data can find tables in both public and public_states schemas
        String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%s/%s?currentSchema=public,public_states",
                POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB);
        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> POSTGRES_USER);
        registry.add("spring.datasource.password", () -> POSTGRES_PASSWORD);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @BeforeAll
    static void checkPostgresAvailability() {
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", POSTGRES_HOST, POSTGRES_PORT, POSTGRES_DB);

        try {
            Class.forName("org.postgresql.Driver");
            try (Connection conn = java.sql.DriverManager.getConnection(jdbcUrl, POSTGRES_USER, POSTGRES_PASSWORD)) {
                System.out.println("[INFO] PostgreSQL available at: " + jdbcUrl);
            }
        } catch (Exception e) {
            throw new IllegalStateException("PostgreSQL not available for integration tests at " + jdbcUrl, e);
        }
    }

    @BeforeEach
    void setupDatabase() throws Exception {
        // Generate DDL from the SDD model
        var yamlLoader = new YamlModelLoader();
        Path modelPath = Path.of("src/main/resources/sdd.yaml");
        var modelResult = yamlLoader.loadFromFile(modelPath);
        assertTrue(modelResult.isSuccess(), "Model should load successfully");
        var model = modelResult.get();

        var generator = DdlGenerators.forDialect("postgres");
        String generatedDdl = generator.generateDdl(model);

        // Clean and recreate schema for each test
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            // Drop and recreate schemas
            stmt.execute("DROP SCHEMA IF EXISTS public_states CASCADE");
            stmt.execute("DROP SCHEMA IF EXISTS public CASCADE");
            stmt.execute("CREATE SCHEMA public");
            stmt.execute("CREATE SCHEMA public_states");

            // Execute the generated DDL
            for (String statement : io.statemodeler.sql.postgres.SqlSplitter.splitSqlStatements(generatedDdl)) {
                statement = statement.trim();
                if (!statement.isEmpty()) {
                    stmt.execute(statement + ";");
                }
            }
        }
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

        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO public.leads (id) VALUES ('" + leadId + "')");
            stmt.execute(String.format(
                    "INSERT INTO public_states.leads_new (lead_id, name, email, phone, source) "
                            + "VALUES ('%s', '%s', '%s', '%s', '%s')",
                    leadId, name, email, phone, source));
        }

        return leadId;
    }

    private void transitionToContactedDirectly(UUID leadId) throws Exception {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT id FROM public_states.leads_new WHERE lead_id = '" + leadId + "'");
            if (rs.next()) {
                Long previousNewId = rs.getLong("id");
                stmt.execute(String.format(
                        "INSERT INTO public_states.leads_contacted "
                                + "(lead_id, previous_new_id, contacted_at, contacted_by, notes) "
                                + "VALUES ('%s', %d, NOW(), 'Test Agent', 'Test notes')",
                        leadId, previousNewId));
            }
        }
    }
}
