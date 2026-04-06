package com.bonitasoft.connectors.zendesk;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ListTicketCommentsConnector extends AbstractZendeskConnector {

    static final String INPUT_TICKET_ID = "ticketId";

    static final String OUTPUT_COMMENTS = "comments";
    static final String OUTPUT_TOTAL_COUNT = "totalCount";
    static final String OUTPUT_HAS_MORE = "hasMore";
    static final String OUTPUT_NEXT_PAGE = "nextPage";

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
        log.info("Listing comments for Zendesk ticket: {}", configuration.getTicketId());
        ZendeskClient.ListTicketCommentsResult result = client.listTicketComments(configuration);
        setOutputParameter(OUTPUT_COMMENTS, result.comments());
        setOutputParameter(OUTPUT_TOTAL_COUNT, result.totalCount());
        setOutputParameter(OUTPUT_HAS_MORE, result.hasMore());
        setOutputParameter(OUTPUT_NEXT_PAGE, result.nextPage());
        log.info("Found {} comments for ticket {}", result.totalCount(), configuration.getTicketId());
    }
}
