package com.bonitasoft.connectors.zendesk;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.bonitasoft.engine.connector.ConnectorValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class UpdateTicketConnectorTest {

    @Mock
    private ZendeskClient mockClient;

    private UpdateTicketConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new UpdateTicketConnector();
        inputs = new HashMap<>();
        inputs.put("subdomain", "mycompany");
        inputs.put("email", "agent@example.com");
        inputs.put("apiToken", "test-token-123");
        inputs.put("authMode", "API_TOKEN");
        inputs.put("ticketId", 12345L);
        inputs.put("status", "open");
    }

    private void injectMockClient() throws Exception {
        connector.setInputParameters(inputs);
        connector.validateInputParameters();
        var clientField = AbstractZendeskConnector.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(connector, mockClient);
    }

    @Test
    void shouldExecuteSuccessfully() throws Exception {
        injectMockClient();
        when(mockClient.updateTicket(any())).thenReturn(
                new ZendeskClient.UpdateTicketResult(12345L, "open", "2024-01-01T12:00:00Z"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("ticketId")).isEqualTo(12345L);
        assertThat(outputs.get("status")).isEqualTo("open");
    }

    @Test
    void shouldFailValidationWhenTicketIdMissing() {
        inputs.remove("ticketId");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldFailValidationWhenSubdomainMissing() {
        inputs.remove("subdomain");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldHandleClientException() throws Exception {
        injectMockClient();
        when(mockClient.updateTicket(any())).thenThrow(
                new ZendeskException("Zendesk API error 404: Ticket not found", 404, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("404");
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.updateTicket(any())).thenReturn(
                new ZendeskClient.UpdateTicketResult(12345L, "pending", "2024-06-15T10:30:00Z"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("ticketId")).isNotNull();
        assertThat(outputs.get("status")).isNotNull();
        assertThat(outputs.get("updatedAt")).isNotNull();
    }
}
