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
class SearchTicketsConnectorTest {

    @Mock
    private ZendeskClient mockClient;

    private SearchTicketsConnector connector;
    private Map<String, Object> inputs;

    @BeforeEach
    void setUp() {
        connector = new SearchTicketsConnector();
        inputs = new HashMap<>();
        inputs.put("subdomain", "mycompany");
        inputs.put("email", "agent@example.com");
        inputs.put("apiToken", "test-token-123");
        inputs.put("authMode", "API_TOKEN");
        inputs.put("query", "status:open");
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
        when(mockClient.searchTickets(any())).thenReturn(
                new ZendeskClient.SearchTicketsResult("[{\"id\":1}]", 1, false, ""));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("totalCount")).isEqualTo(1);
        assertThat((String) outputs.get("tickets")).contains("\"id\"");
    }

    @Test
    void shouldFailValidationWhenQueryMissing() {
        inputs.remove("query");
        connector.setInputParameters(inputs);
        assertThatThrownBy(() -> connector.validateInputParameters())
                .isInstanceOf(ConnectorValidationException.class);
    }

    @Test
    void shouldHandleClientException() throws Exception {
        injectMockClient();
        when(mockClient.searchTickets(any())).thenThrow(
                new ZendeskException("Zendesk API error 422: Invalid query", 422, false));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(false);
        assertThat((String) outputs.get("errorMessage")).contains("422");
    }

    @Test
    void shouldPopulateAllOutputFields() throws Exception {
        injectMockClient();
        when(mockClient.searchTickets(any())).thenReturn(
                new ZendeskClient.SearchTicketsResult("[]", 0, false, ""));
        connector.executeBusinessLogic();
        var outputs = connector.getOutputs();
        assertThat(outputs.get("success")).isEqualTo(true);
        assertThat(outputs.get("tickets")).isNotNull();
        assertThat(outputs.get("totalCount")).isNotNull();
        assertThat(outputs.get("hasMore")).isNotNull();
        assertThat(outputs.get("nextPage")).isNotNull();
    }
}
