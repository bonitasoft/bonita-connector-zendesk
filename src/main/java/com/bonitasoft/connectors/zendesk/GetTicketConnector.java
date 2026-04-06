package com.bonitasoft.connectors.zendesk;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GetTicketConnector extends AbstractZendeskConnector {

    static final String INPUT_TICKET_ID = "ticketId";

    static final String OUTPUT_TICKET_ID = "ticketId";
    static final String OUTPUT_SUBJECT = "subject";
    static final String OUTPUT_STATUS = "status";
    static final String OUTPUT_PRIORITY = "priority";
    static final String OUTPUT_TYPE = "type";
    static final String OUTPUT_CREATED_AT = "createdAt";
    static final String OUTPUT_UPDATED_AT = "updatedAt";
    static final String OUTPUT_ALL_FIELDS = "allFields";

    @Override
    protected ZendeskConfiguration buildConfiguration() {
        return connectionConfig()
                .ticketId(readLongInput(INPUT_TICKET_ID))
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
        log.info("Getting Zendesk ticket: {}", configuration.getTicketId());
        ZendeskClient.GetTicketResult result = client.getTicket(configuration);
        setOutputParameter(OUTPUT_TICKET_ID, result.ticketId());
        setOutputParameter(OUTPUT_SUBJECT, result.subject());
        setOutputParameter(OUTPUT_STATUS, result.status());
        setOutputParameter(OUTPUT_PRIORITY, result.priority());
        setOutputParameter(OUTPUT_TYPE, result.type());
        setOutputParameter(OUTPUT_CREATED_AT, result.createdAt());
        setOutputParameter(OUTPUT_UPDATED_AT, result.updatedAt());
        setOutputParameter(OUTPUT_ALL_FIELDS, result.allFields());
        log.info("Ticket {} retrieved successfully", result.ticketId());
    }
}
