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
class GetTicketConnectorTest {

    @Mock
    private ZendeskClient mockClient;

    private GetTicketConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new GetTicketConnector();
        inputs = new HashMap<>();
        inputs.put("subdomain", "mycompany");
        inputs.put("email", "agent@example.com");
        inputs.put("apiToken", "test-token-123");
        inputs.put("authMode", "API_TOKEN");
        inputs.put("ticketId", 12345L);
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
        when(mockClient.getTicket(any())).thenReturn(
                new ZendeskClient.GetTicketResult(12345L, "Test Subject", "open", "high",
                        "incident", "2024-01-01T00:00:00Z", "2024-01-02T00:00:00Z", "{}"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("ticketId")).isEqualTo(12345L);
        assertThat(outputs.get("subject")).isEqualTo("Test Subject");
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
    void shouldHandleClientException() throws Exception {
        injectMockClient();
        when(mockClient.getTicket(any())).thenThrow(
                new ZendeskException("Zendesk API error 404: Not found", 404, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("404");
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.getTicket(any())).thenReturn(
                new ZendeskClient.GetTicketResult(12345L, "Subject", "open", "normal",
                        "question", "2024-01-01", "2024-01-02", "{\"id\":12345}"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("ticketId")).isNotNull();
        assertThat(outputs.get("subject")).isNotNull();
        assertThat(outputs.get("status")).isNotNull();
        assertThat(outputs.get("priority")).isNotNull();
        assertThat(outputs.get("type")).isNotNull();
        assertThat(outputs.get("createdAt")).isNotNull();
        assertThat(outputs.get("updatedAt")).isNotNull();
        assertThat(outputs.get("allFields")).isNotNull();
    }
}
