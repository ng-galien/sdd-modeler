package com.example.leadcrm.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GeneratedMcpServerIT {

    @Test
    void mcpServerDelegatesToServiceAndBuildsResourceAndPrompt() {
        var service = new StubLeadService();
        var objectMapper = new ObjectMapper();
        var server = new LeadMcpServer(service, objectMapper);

        var leadUuid = UUID.randomUUID();
        var dto = server.transitionToQualified(leadUuid, new BigDecimal("1200.00"), "Q1", "notes");
        assertNotNull(dto);
        assertEquals(leadUuid, dto.id().value());
        assertNotNull(service.lastQualifiedCommand);
        assertEquals(new BigDecimal("1200.00"), service.lastQualifiedCommand.budget());

        var leadId = new LeadId(leadUuid);
        var newState = new LeadState.New(null, leadId, "Alice", "a@example.com", "555-1111", "web");
        service.setStates(List.of(new LeadService.LeadStateInfo("NEW", newState)));

        ReadResourceResult resourceResult = server.getLeadStatesResource();
        assertEquals(1, resourceResult.contents().size());
        var textResource = (TextResourceContents) resourceResult.contents().get(0);
        assertEquals("sdd://lead/states", textResource.uri());
        assertEquals("application/json", textResource.mimeType());
        assertTrue(textResource.text().contains("NEW"));

        var promptResult = server.leadSummaryPrompt();
        assertEquals("Lead Summary", promptResult.description());
        assertEquals(1, promptResult.messages().size());
        PromptMessage message = promptResult.messages().get(0);
        var content = (TextContent) message.content();
        assertTrue(content.text().contains("Lead states: 1"));
    }

    static final class StubLeadService implements LeadService {

        private List<LeadStateInfo> states = List.of();
        private TransitionToQualifiedCommand lastQualifiedCommand;

        @Override
        public List<LeadStateInfo> findAll() {
            return states;
        }

        void setStates(List<LeadStateInfo> states) {
            this.states = states;
        }

        @Override
        public LeadDto transitionToContacted(LeadId id, TransitionToContactedCommand command) {
            return new LeadDto(id);
        }

        @Override
        public LeadDto transitionToQualified(LeadId id, TransitionToQualifiedCommand command) {
            this.lastQualifiedCommand = command;
            return new LeadDto(id);
        }

        @Override
        public LeadDto transitionToConverted(LeadId id, TransitionToConvertedCommand command) {
            return new LeadDto(id);
        }
    }
}
