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
class ListTicketCommentsConnectorTest {

    @Mock
    private ZendeskClient mockClient;

    private ListTicketCommentsConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new ListTicketCommentsConnector();
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
        when(mockClient.listTicketComments(any())).thenReturn(
                new ZendeskClient.ListTicketCommentsResult("[{\"id\":1,\"body\":\"Hello\"}]", 1, false, ""));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("totalCount")).isEqualTo(1);
        assertThat((String) outputs.get("comments")).contains("Hello");
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
        when(mockClient.listTicketComments(any())).thenThrow(
                new ZendeskException("Zendesk API error 404: Ticket not found", 404, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("404");
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.listTicketComments(any())).thenReturn(
                new ZendeskClient.ListTicketCommentsResult("[]", 0, false, ""));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("comments")).isNotNull();
        assertThat(outputs.get("totalCount")).isNotNull();
        assertThat(outputs.get("hasMore")).isNotNull();
        assertThat(outputs.get("nextPage")).isNotNull();
    }
}
