package com.bonitasoft.connectors.zendesk;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SearchTicketsConnector extends AbstractZendeskConnector {

    static final String INPUT_QUERY = "query";
    static final String INPUT_SORT_BY = "sortBy";
    static final String INPUT_SORT_ORDER = "sortOrder";

    static final String OUTPUT_TICKETS = "tickets";
    static final String OUTPUT_TOTAL_COUNT = "totalCount";
    static final String OUTPUT_HAS_MORE = "hasMore";
    static final String OUTPUT_NEXT_PAGE = "nextPage";

    @Override
    protected ZendeskConfiguration buildConfiguration() {
        return connectionConfig()
                .query(readStringInput(INPUT_QUERY))
                .sortBy(readStringInput(INPUT_SORT_BY))
                .sortOrder(readStringInput(INPUT_SORT_ORDER))
                .build();
    }

    @Override
    protected void validateConfiguration(ZendeskConfiguration config) {
        super.validateConfiguration(config);
        if (config.getQuery() == null || config.getQuery().isBlank()) {
            throw new IllegalArgumentException("query is mandatory");
        }
    }

    @Override
    protected void doExecute() throws ZendeskException {
        log.info("Searching Zendesk tickets: {}", configuration.getQuery());
        ZendeskClient.SearchTicketsResult result = client.searchTickets(configuration);
        setOutputParameter(OUTPUT_TICKETS, result.tickets());
        setOutputParameter(OUTPUT_TOTAL_COUNT, result.totalCount());
        setOutputParameter(OUTPUT_HAS_MORE, result.hasMore());
        setOutputParameter(OUTPUT_NEXT_PAGE, result.nextPage());
        log.info("Search returned {} results", result.totalCount());
    }
}
