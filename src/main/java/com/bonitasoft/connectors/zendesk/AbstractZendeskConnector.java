package com.bonitasoft.connectors.zendesk;

import lombok.extern.slf4j.Slf4j;
import org.bonitasoft.engine.connector.AbstractConnector;
import org.bonitasoft.engine.connector.ConnectorException;
import org.bonitasoft.engine.connector.ConnectorValidationException;

import java.util.Map;

/**
 * Abstract base connector for Zendesk Support operations.
 * Handles lifecycle: validate -> connect -> execute -> disconnect.
 * Subclasses implement buildConfiguration() and doExecute().
 */
@Slf4j
public abstract class AbstractZendeskConnector extends AbstractConnector {

    protected static final String OUTPUT_SUCCESS = "success";
    protected static final String OUTPUT_ERROR_MESSAGE = "errorMessage";

    protected ZendeskConfiguration configuration;
    protected ZendeskClient client;

    @Override
    public void validateInputParameters() throws ConnectorValidationException {
        try {
            this.configuration = buildConfiguration();
            validateConfiguration(this.configuration);
        } catch (IllegalArgumentException e) {
            throw new ConnectorValidationException(this, e.getMessage());
        }
    }

    @Override
    public void connect() throws ConnectorException {
        try {
            this.client = new ZendeskClient(this.configuration);
            log.info("Zendesk connector connected successfully");
        } catch (ZendeskException e) {
            throw new ConnectorException("Failed to connect: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() throws ConnectorException {
        this.client = null;
    }

    @Override
    protected void executeBusinessLogic() throws ConnectorException {
        try {
            doExecute();
            setOutputParameter(OUTPUT_SUCCESS, true);
        } catch (ZendeskException e) {
            log.error("Zendesk connector execution failed: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in Zendesk connector: {}", e.getMessage(), e);
            setOutputParameter(OUTPUT_SUCCESS, false);
            setOutputParameter(OUTPUT_ERROR_MESSAGE, "Unexpected error: " + e.getMessage());
        }
    }

    protected abstract void doExecute() throws ZendeskException;

    protected abstract ZendeskConfiguration buildConfiguration();

    /**
     * Validates the configuration. Checks auth requirements.
     */
    protected void validateConfiguration(ZendeskConfiguration config) {
        if (config.getSubdomain() == null || config.getSubdomain().isBlank()) {
            throw new IllegalArgumentException("subdomain is mandatory");
        }
        String authMode = config.getAuthMode();
        if ("API_TOKEN".equals(authMode)) {
            String email = resolveValue(config.getEmail(), "zendesk.email", "ZENDESK_EMAIL");
            String apiToken = resolveValue(config.getApiToken(), "zendesk.apiToken", "ZENDESK_API_TOKEN");
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("email is mandatory when authMode=API_TOKEN");
            }
            if (apiToken == null || apiToken.isBlank()) {
                throw new IllegalArgumentException("apiToken is mandatory when authMode=API_TOKEN");
            }
        } else if ("OAUTH2".equals(authMode)) {
            String apiToken = resolveValue(config.getApiToken(), "zendesk.apiToken", "ZENDESK_API_TOKEN");
            if (apiToken == null || apiToken.isBlank()) {
                throw new IllegalArgumentException("apiToken (OAuth2 access token) is mandatory when authMode=OAUTH2");
            }
        } else if (authMode != null && !authMode.isBlank()) {
            throw new IllegalArgumentException("Invalid authMode: " + authMode + ". Must be API_TOKEN or OAUTH2");
        }
    }

    /** Resolves a value from direct input, JVM system property, or environment variable. */
    protected String resolveValue(String directValue, String sysProp, String envVar) {
        if (directValue != null && !directValue.isBlank()) {
            return directValue;
        }
        String sysValue = System.getProperty(sysProp);
        if (sysValue != null && !sysValue.isBlank()) {
            return sysValue;
        }
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return null;
    }

    /** Helper: read a String input, returning null if not set. */
    protected String readStringInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? value.toString() : null;
    }

    /** Helper: read a String input with a default value. */
    protected String readStringInput(String name, String defaultValue) {
        String value = readStringInput(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    /** Helper: read a Boolean input with a default value. */
    protected Boolean readBooleanInput(String name, boolean defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? (Boolean) value : defaultValue;
    }

    /** Helper: read an Integer input with a default value. */
    protected Integer readIntegerInput(String name, int defaultValue) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).intValue() : defaultValue;
    }

    /** Helper: read a Long input, returning null if not set. */
    protected Long readLongInput(String name) {
        Object value = getInputParameter(name);
        return value != null ? ((Number) value).longValue() : null;
    }

    /** Connection config builder with shared auth params. */
    protected ZendeskConfiguration.ZendeskConfigurationBuilder connectionConfig() {
        return ZendeskConfiguration.builder()
                .subdomain(readStringInput("subdomain"))
                .email(readStringInput("email"))
                .apiToken(readStringInput("apiToken"))
                .authMode(readStringInput("authMode", "API_TOKEN"))
                .connectTimeout(readIntegerInput("connectTimeout", 30000))
                .readTimeout(readIntegerInput("readTimeout", 60000));
    }

    /**
     * Exposes output parameters for testing. Package-visible.
     */
    Map<String, Object> getOutputs() {
        return getOutputParameters();
    }
}
