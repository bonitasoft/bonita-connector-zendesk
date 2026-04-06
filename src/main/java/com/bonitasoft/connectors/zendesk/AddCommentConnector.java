package com.bonitasoft.connectors.zendesk;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AddCommentConnector extends AbstractZendeskConnector {

    static final String INPUT_TICKET_ID = "ticketId";
    static final String INPUT_COMMENT = "comment";
    static final String INPUT_IS_PUBLIC = "isPublic";
    static final String INPUT_AUTHOR_EMAIL = "authorEmail";

    static final String OUTPUT_TICKET_ID = "ticketId";
    static final String OUTPUT_UPDATED_AT = "updatedAt";

    @Override
    protected ZendeskConfiguration buildConfiguration() {
        return connectionConfig()
                .ticketId(readLongInput(INPUT_TICKET_ID))
                .comment(readStringInput(INPUT_COMMENT))
                .isPublic(readBooleanInput(INPUT_IS_PUBLIC, true))
                .authorEmail(readStringInput(INPUT_AUTHOR_EMAIL))
                .build();
    }

    @Override
    protected void validateConfiguration(ZendeskConfiguration config) {
        super.validateConfiguration(config);
        if (config.getTicketId() == null) {
            throw new IllegalArgumentException("ticketId is mandatory");
        }
        if (config.getComment() == null || config.getComment().isBlank()) {
            throw new IllegalArgumentException("comment is mandatory");
        }
    }

    @Override
    protected void doExecute() throws ZendeskException {
        log.info("Adding comment to Zendesk ticket: {}", configuration.getTicketId());
        ZendeskClient.AddCommentResult result = client.addComment(configuration);
        setOutputParameter(OUTPUT_TICKET_ID, result.ticketId());
        setOutputParameter(OUTPUT_UPDATED_AT, result.updatedAt());
        log.info("Comment added to ticket {}", result.ticketId());
    }
}
