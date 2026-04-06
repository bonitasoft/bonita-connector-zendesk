package com.bonitasoft.connectors.zendesk;

/**
 * Typed exception for Zendesk connector operations.
 */
public class ZendeskException extends Exception {

    private final int statusCode;
    private final boolean retryable;

    public ZendeskException(String message) {
        super(message);
        this.statusCode = -1;
        this.retryable = false;
    }

    public ZendeskException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.retryable = false;
    }

    public ZendeskException(String message, int statusCode, boolean retryable) {
        super(message);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public ZendeskException(String message, int statusCode, boolean retryable, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryable = retryable;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
