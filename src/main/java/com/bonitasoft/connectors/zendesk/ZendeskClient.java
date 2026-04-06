package com.bonitasoft.connectors.zendesk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * HTTP client facade for Zendesk Support API v2.
 * Uses java.net.http.HttpClient with Basic Auth (email/token:apiToken) or OAuth2 Bearer token.
 * All methods use the RetryPolicy for automatic exponential backoff on 429/5xx.
 */
@Slf4j
public class ZendeskClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ZendeskConfiguration configuration;
    private final HttpClient httpClient;
    private final RetryPolicy retryPolicy;
    private final String baseUrl;
    private final String authHeader;

    public ZendeskClient(ZendeskConfiguration configuration) throws ZendeskException {
        this.configuration = configuration;
        this.retryPolicy = new RetryPolicy(configuration.getMaxRetries());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(configuration.getConnectTimeout()))
                .build();
        this.baseUrl = "https://" + configuration.getSubdomain() + ".zendesk.com/api/v2";
        this.authHeader = buildAuthHeader();
        log.debug("ZendeskClient initialized for subdomain={} authMode={}",
                configuration.getSubdomain(), configuration.getAuthMode());
    }

    // Visible for testing
    ZendeskClient(ZendeskConfiguration configuration, HttpClient httpClient,
                  RetryPolicy retryPolicy, String baseUrl, String authHeader) {
        this.configuration = configuration;
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy;
        this.baseUrl = baseUrl;
        this.authHeader = authHeader;
    }

    private String buildAuthHeader() throws ZendeskException {
        if ("OAUTH2".equals(configuration.getAuthMode())) {
            String token = resolveValue(configuration.getApiToken(), "zendesk.apiToken", "ZENDESK_API_TOKEN");
            if (token == null || token.isBlank()) {
                throw new ZendeskException("OAuth2 access token not found");
            }
            return "Bearer " + token;
        } else {
            // API Token auth: base64(email/token:apiToken)
            String email = resolveValue(configuration.getEmail(), "zendesk.email", "ZENDESK_EMAIL");
            String apiToken = resolveValue(configuration.getApiToken(), "zendesk.apiToken", "ZENDESK_API_TOKEN");
            if (email == null || email.isBlank() || apiToken == null || apiToken.isBlank()) {
                throw new ZendeskException("email and apiToken are required for API Token authentication");
            }
            String credentials = email + "/token:" + apiToken;
            return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String resolveValue(String directValue, String sysProp, String envVar) {
        if (directValue != null && !directValue.isBlank()) return directValue;
        String sysValue = System.getProperty(sysProp);
        if (sysValue != null && !sysValue.isBlank()) return sysValue;
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isBlank()) return envValue;
        return null;
    }

    // === Ticket Operations ===

    public CreateTicketResult createTicket(ZendeskConfiguration config) throws ZendeskException {
        return retryPolicy.execute(() -> {
            ObjectNode ticket = MAPPER.createObjectNode();
            ticket.put("subject", config.getSubject());
            if (config.getDescription() != null && !config.getDescription().isBlank()) {
                ObjectNode comment = MAPPER.createObjectNode();
                comment.put("body", config.getDescription());
                ticket.set("comment", comment);
            }
            if (config.getRequesterEmail() != null && !config.getRequesterEmail().isBlank()) {
                ObjectNode requester = MAPPER.createObjectNode();
                requester.put("email", config.getRequesterEmail());
                ticket.set("requester", requester);
            }
            if (config.getPriority() != null && !config.getPriority().isBlank()) {
                ticket.put("priority", config.getPriority());
            }
            if (config.getType() != null && !config.getType().isBlank()) {
                ticket.put("type", config.getType());
            }
            if (config.getTags() != null && !config.getTags().isBlank()) {
                ArrayNode tagsArray = MAPPER.createArrayNode();
                for (String tag : config.getTags().split(",")) {
                    tagsArray.add(tag.trim());
                }
                ticket.set("tags", tagsArray);
            }
            mergeCustomFields(ticket, config.getCustomFields());

            ObjectNode body = MAPPER.createObjectNode();
            body.set("ticket", ticket);

            JsonNode response = doPost("/tickets.json", body);
            JsonNode t = response.get("ticket");
            return new CreateTicketResult(
                    t.get("id").asLong(),
                    getTextOrNull(t, "status"),
                    getTextOrNull(t, "created_at"),
                    buildTicketUrl(t.get("id").asLong())
            );
        });
    }

    public UpdateTicketResult updateTicket(ZendeskConfiguration config) throws ZendeskException {
        return retryPolicy.execute(() -> {
            ObjectNode ticket = MAPPER.createObjectNode();
            if (config.getSubject() != null && !config.getSubject().isBlank()) {
                ticket.put("subject", config.getSubject());
            }
            if (config.getStatus() != null && !config.getStatus().isBlank()) {
                ticket.put("status", config.getStatus());
            }
            if (config.getPriority() != null && !config.getPriority().isBlank()) {
                ticket.put("priority", config.getPriority());
            }
            if (config.getType() != null && !config.getType().isBlank()) {
                ticket.put("type", config.getType());
            }
            if (config.getAssigneeEmail() != null && !config.getAssigneeEmail().isBlank()) {
                ticket.put("assignee_email", config.getAssigneeEmail());
            }
            if (config.getTags() != null && !config.getTags().isBlank()) {
                ArrayNode tagsArray = MAPPER.createArrayNode();
                for (String tag : config.getTags().split(",")) {
                    tagsArray.add(tag.trim());
                }
                ticket.set("tags", tagsArray);
            }
            mergeCustomFields(ticket, config.getCustomFields());

            ObjectNode body = MAPPER.createObjectNode();
            body.set("ticket", ticket);

            JsonNode response = doPut("/tickets/" + config.getTicketId() + ".json", body);
            JsonNode t = response.get("ticket");
            return new UpdateTicketResult(
                    t.get("id").asLong(),
                    getTextOrNull(t, "status"),
                    getTextOrNull(t, "updated_at")
            );
        });
    }

    public GetTicketResult getTicket(ZendeskConfiguration config) throws ZendeskException {
        return retryPolicy.execute(() -> {
            JsonNode response = doGet("/tickets/" + config.getTicketId() + ".json");
            JsonNode t = response.get("ticket");
            String allFieldsJson = MAPPER.writeValueAsString(t);
            return new GetTicketResult(
                    t.get("id").asLong(),
                    getTextOrNull(t, "subject"),
                    getTextOrNull(t, "status"),
                    getTextOrNull(t, "priority"),
                    getTextOrNull(t, "type"),
                    getTextOrNull(t, "created_at"),
                    getTextOrNull(t, "updated_at"),
                    allFieldsJson
            );
        });
    }

    public AddCommentResult addComment(ZendeskConfiguration config) throws ZendeskException {
        return retryPolicy.execute(() -> {
            ObjectNode comment = MAPPER.createObjectNode();
            comment.put("body", config.getComment());
            comment.put("public", Boolean.TRUE.equals(config.getIsPublic()));
            if (config.getAuthorEmail() != null && !config.getAuthorEmail().isBlank()) {
                comment.put("author_id", config.getAuthorEmail());
            }

            ObjectNode ticket = MAPPER.createObjectNode();
            ticket.set("comment", comment);

            ObjectNode body = MAPPER.createObjectNode();
            body.set("ticket", ticket);

            JsonNode response = doPut("/tickets/" + config.getTicketId() + ".json", body);
            JsonNode t = response.get("ticket");
            return new AddCommentResult(
                    t.get("id").asLong(),
                    getTextOrNull(t, "updated_at")
            );
        });
    }

    public SearchTicketsResult searchTickets(ZendeskConfiguration config) throws ZendeskException {
        return retryPolicy.execute(() -> {
            String query = URLEncoder.encode(config.getQuery(), StandardCharsets.UTF_8);
            StringBuilder path = new StringBuilder("/search.json?query=type:ticket+" + query);
            if (config.getSortBy() != null && !config.getSortBy().isBlank()) {
                path.append("&sort_by=").append(URLEncoder.encode(config.getSortBy(), StandardCharsets.UTF_8));
            }
            if (config.getSortOrder() != null && !config.getSortOrder().isBlank()) {
                path.append("&sort_order=").append(URLEncoder.encode(config.getSortOrder(), StandardCharsets.UTF_8));
            }

            JsonNode response = doGet(path.toString());
            int count = response.has("count") ? response.get("count").asInt() : 0;
            boolean hasMore = response.has("next_page") && !response.get("next_page").isNull();
            String nextPage = hasMore ? response.get("next_page").asText() : "";

            JsonNode results = response.get("results");
            String ticketsJson = results != null ? MAPPER.writeValueAsString(results) : "[]";

            return new SearchTicketsResult(
                    ticketsJson,
                    count,
                    hasMore,
                    nextPage
            );
        });
    }

    public ListTicketCommentsResult listTicketComments(ZendeskConfiguration config) throws ZendeskException {
        return retryPolicy.execute(() -> {
            JsonNode response = doGet("/tickets/" + config.getTicketId() + "/comments.json");

            int count = response.has("count") ? response.get("count").asInt() : 0;
            boolean hasMore = response.has("next_page") && !response.get("next_page").isNull();
            String nextPage = hasMore ? response.get("next_page").asText() : "";

            JsonNode comments = response.get("comments");
            String commentsJson = comments != null ? MAPPER.writeValueAsString(comments) : "[]";

            return new ListTicketCommentsResult(
                    commentsJson,
                    count,
                    hasMore,
                    nextPage
            );
        });
    }

    // === HTTP Methods ===

    private JsonNode doGet(String path) throws ZendeskException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .GET()
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (ZendeskException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ZendeskException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private JsonNode doPost(String path, ObjectNode body) throws ZendeskException {
        try {
            String jsonBody = MAPPER.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (ZendeskException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ZendeskException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private JsonNode doPut(String path, ObjectNode body) throws ZendeskException {
        try {
            String jsonBody = MAPPER.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofMillis(configuration.getReadTimeout()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return handleResponse(response);
        } catch (ZendeskException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ZendeskException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private JsonNode handleResponse(HttpResponse<String> response) throws ZendeskException {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode >= 200 && statusCode < 300) {
            try {
                return MAPPER.readTree(responseBody);
            } catch (JsonProcessingException e) {
                throw new ZendeskException("Failed to parse response: " + e.getMessage(), e);
            }
        }

        boolean retryable = RetryPolicy.isRetryableStatusCode(statusCode);
        String errorMessage = buildErrorMessage(statusCode, responseBody);
        throw new ZendeskException(errorMessage, statusCode, retryable);
    }

    private String buildErrorMessage(int statusCode, String responseBody) {
        try {
            JsonNode error = MAPPER.readTree(responseBody);
            if (error.has("error")) {
                String title = getTextOrNull(error, "error");
                String desc = getTextOrNull(error, "description");
                return String.format("Zendesk API error %d (%s): %s", statusCode,
                        title != null ? title : "UNKNOWN",
                        desc != null ? desc : responseBody);
            }
        } catch (JsonProcessingException ignored) {
            // Fall through to generic message
        }
        return "Zendesk API error " + statusCode + ": " + responseBody;
    }

    private void mergeCustomFields(ObjectNode ticket, String customFieldsJson) throws ZendeskException {
        if (customFieldsJson == null || customFieldsJson.isBlank()) return;
        try {
            JsonNode custom = MAPPER.readTree(customFieldsJson);
            if (custom.isArray()) {
                ticket.set("custom_fields", custom);
            }
        } catch (JsonProcessingException e) {
            throw new ZendeskException("Invalid customFields JSON: " + e.getMessage(), e);
        }
    }

    private String getTextOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    private String buildTicketUrl(long ticketId) {
        return "https://" + configuration.getSubdomain() + ".zendesk.com/agent/tickets/" + ticketId;
    }

    // === Result records ===

    public record CreateTicketResult(long ticketId, String status, String createdAt, String ticketUrl) {}

    public record UpdateTicketResult(long ticketId, String status, String updatedAt) {}

    public record GetTicketResult(long ticketId, String subject, String status, String priority,
                                  String type, String createdAt, String updatedAt,
                                  String allFields) {}

    public record AddCommentResult(long ticketId, String updatedAt) {}

    public record SearchTicketsResult(String tickets, int totalCount, boolean hasMore, String nextPage) {}

    public record ListTicketCommentsResult(String comments, int totalCount, boolean hasMore, String nextPage) {}
}
