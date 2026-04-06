package com.bonitasoft.connectors.zendesk;

import lombok.Builder;
import lombok.Data;

/**
 * Configuration for all Zendesk connector operations.
 * Holds connection, authentication, and operation-specific parameters.
 */
@Data
@Builder
public class ZendeskConfiguration {

    // Connection / Auth
    private String subdomain;
    private String email;
    private String apiToken;
    @Builder.Default
    private String authMode = "API_TOKEN";
    @Builder.Default
    private int connectTimeout = 30000;
    @Builder.Default
    private int readTimeout = 60000;
    @Builder.Default
    private int maxRetries = 5;

    // Create Ticket
    private String subject;
    private String description;
    private String requesterEmail;
    private String priority;
    private String type;
    private String tags;
    private String customFields;

    // Update Ticket / Get Ticket / Add Comment / List Comments
    private Long ticketId;
    private String status;
    private String assigneeEmail;

    // Add Comment
    private String comment;
    @Builder.Default
    private Boolean isPublic = true;
    private String authorEmail;

    // Search Tickets
    private String query;
    private String sortBy;
    private String sortOrder;
}
