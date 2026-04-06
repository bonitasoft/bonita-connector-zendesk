package com.bonitasoft.connectors.zendesk;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CreateTicketConnector extends AbstractZendeskConnector {

    static final String INPUT_SUBJECT = "subject";
    static final String INPUT_DESCRIPTION = "description";
    static final String INPUT_REQUESTER_EMAIL = "requesterEmail";
    static final String INPUT_PRIORITY = "priority";
    static final String INPUT_TYPE = "type";
    static final String INPUT_TAGS = "tags";
    static final String INPUT_CUSTOM_FIELDS = "customFields";

    static final String OUTPUT_TICKET_ID = "ticketId";
    static final String OUTPUT_STATUS = "status";
    static final String OUTPUT_CREATED_AT = "createdAt";
    static final String OUTPUT_TICKET_URL = "ticketUrl";

    @Override
    protected ZendeskConfiguration buildConfiguration() {
        return connectionConfig()
                .subject(readStringInput(INPUT_SUBJECT))
                .description(readStringInput(INPUT_DESCRIPTION))
                .requesterEmail(readStringInput(INPUT_REQUESTER_EMAIL))
                .priority(readStringInput(INPUT_PRIORITY))
                .type(readStringInput(INPUT_TYPE))
                .tags(readStringInput(INPUT_TAGS))
                .customFields(readStringInput(INPUT_CUSTOM_FIELDS))
                .build();
    }

    @Override
    protected void validateConfiguration(ZendeskConfiguration config) {
        super.validateConfiguration(config);
        if (config.getSubject() == null || config.getSubject().isBlank()) {
            throw new IllegalArgumentException("subject is mandatory");
        }
    }

    @Override
    protected void doExecute() throws ZendeskException {
        log.info("Creating Zendesk ticket: {}", configuration.getSubject());
        ZendeskClient.CreateTicketResult result = client.createTicket(configuration);
        setOutputParameter(OUTPUT_TICKET_ID, result.ticketId());
        setOutputParameter(OUTPUT_STATUS, result.status());
        setOutputParameter(OUTPUT_CREATED_AT, result.createdAt());
        setOutputParameter(OUTPUT_TICKET_URL, result.ticketUrl());
        log.info("Ticket created: {}", result.ticketUrl());
    }
}
