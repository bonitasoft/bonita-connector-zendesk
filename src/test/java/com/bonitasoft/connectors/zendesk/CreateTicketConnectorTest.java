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
class CreateTicketConnectorTest {

    @Mock
    private ZendeskClient mockClient;

    private CreateTicketConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new CreateTicketConnector();
        inputs = new HashMap<>();
        inputs.put("subdomain", "mycompany");
        inputs.put("email", "agent@example.com");
        inputs.put("apiToken", "test-token-123");
        inputs.put("authMode", "API_TOKEN");
        inputs.put("subject", "Test ticket subject");
        inputs.put("description", "Test ticket description");
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
        when(mockClient.createTicket(any())).thenReturn(
                new ZendeskClient.CreateTicketResult(12345L, "new", "2024-01-01T00:00:00Z",
                        "https://mycompany.zendesk.com/agent/tickets/12345"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("ticketId")).isEqualTo(12345L);
        assertThat(outputs.get("status")).isEqualTo("new");
        assertThat(outputs.get("ticketUrl")).isEqualTo("https://mycompany.zendesk.com/agent/tickets/12345");
    }

    @Test
    void shouldFailValidationWhenSubjectMissing() {
        inputs.remove("subject");
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
        when(mockClient.createTicket(any())).thenThrow(
                new ZendeskException("Zendesk API error 401 (Unauthorized): Invalid credentials", 401, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("401");
    }

    @Test
    void shouldHandleUnexpectedException() throws Exception {
        injectMockClient();
        when(mockClient.createTicket(any())).thenThrow(new RuntimeException("Network failure"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("Unexpected");
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.createTicket(any())).thenReturn(
                new ZendeskClient.CreateTicketResult(99L, "open", "2024-06-15T10:30:00Z",
                        "https://mycompany.zendesk.com/agent/tickets/99"));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("ticketId")).isNotNull();
        assertThat(outputs.get("status")).isNotNull();
        assertThat(outputs.get("createdAt")).isNotNull();
        assertThat(outputs.get("ticketUrl")).isNotNull();
    }

    @Test
    void shouldApplyDefaultsForOptionalInputs() throws Exception {
        var minimalInputs = new HashMap<String, Object>();
        minimalInputs.put("subdomain", "mycompany");
        minimalInputs.put("email", "agent@example.com");
        minimalInputs.put("apiToken", "test-token");
        minimalInputs.put("subject", "Test");
        connector.setInputParameters(minimalInputs);
        connector.validateInputParameters();
        var configField = AbstractZendeskConnector.class.getDeclaredField("configuration");
        configField.setAccessible(true);
        var config = (ZendeskConfiguration) configField.get(connector);
        assertThat(config.getConnectTimeout()).isEqualTo(30000);
        assertThat(config.getReadTimeout()).isEqualTo(60000);
        assertThat(config.getAuthMode()).isEqualTo("API_TOKEN");
    }
}
