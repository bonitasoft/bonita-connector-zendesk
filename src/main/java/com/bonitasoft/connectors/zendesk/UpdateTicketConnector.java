package com.bonitasoft.connectors.zendesk;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateTicketConnector extends AbstractZendeskConnector {

    static final String INPUT_TICKET_ID = "ticketId";
    static final String INPUT_SUBJECT = "subject";
    static final String INPUT_STATUS = "status";
    static final String INPUT_PRIORITY = "priority";
    static final String INPUT_TYPE = "type";
    static final String INPUT_ASSIGNEE_EMAIL = "assigneeEmail";
    static final String INPUT_TAGS = "tags";
    static final String INPUT_CUSTOM_FIELDS = "customFields";

    static final String OUTPUT_TICKET_ID = "ticketId";
    static final String OUTPUT_STATUS = "status";
    static final String OUTPUT_UPDATED_AT = "updatedAt";

    @Override
    protected ZendeskConfiguration buildConfiguration() {
        return connectionConfig()
                .ticketId(readLongInput(INPUT_TICKET_ID))
                .subject(readStringInput(INPUT_SUBJECT))
                .status(readStringInput(INPUT_STATUS))
                .priority(readStringInput(INPUT_PRIORITY))
                .type(readStringInput(INPUT_TYPE))
                .assigneeEmail(readStringInput(INPUT_ASSIGNEE_EMAIL))
                .tags(readStringInput(INPUT_TAGS))
                .customFields(readStringInput(INPUT_CUSTOM_FIELDS))
                .build();
    }

    @Override
    protected void validateConfiguration(ZendeskConfiguration config) {
        super.validateConfiguration(config);
        if (config.getTicketId() == null) {
            throw new IllegalArgumentException("ticketId is mandatory");
        }
    }

    @Override
    protected void doExecute() throws ZendeskException {
        log.info("Updating Zendesk ticket: {}", configuration.getTicketId());
        ZendeskClient.UpdateTicketResult result = client.updateTicket(configuration);
        setOutputParameter(OUTPUT_TICKET_ID, result.ticketId());
        setOutputParameter(OUTPUT_STATUS, result.status());
        setOutputParameter(OUTPUT_UPDATED_AT, result.updatedAt());
        log.info("Ticket {} updated successfully", result.ticketId());
    }
}
