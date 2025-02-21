package com.company.app.ordermanager.search.service.impl;

import com.company.app.ordermanager.entity.order.Order;
import com.company.app.ordermanager.search.document.OrderDocument;
import com.company.app.ordermanager.search.dto.OrderSearchRequest;
import com.company.app.ordermanager.search.dto.OrderSearchResult;
import com.company.app.ordermanager.search.exception.SearchException;
import com.company.app.ordermanager.search.service.api.OrderSearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.SearchResultPaginated;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeilisearchOrderSearchService implements OrderSearchService {
    private static final long DEFAULT_PAGE_NUMBER = 1;
    private static final String ORDER_INDEX = "orders";

    private final Client meilisearchClient;
    private final ObjectMapper objectMapper;

    private Index orderIndex;

    @PostConstruct
    public void init() {
        this.orderIndex = meilisearchClient.index(ORDER_INDEX);

        configureIndex();
    }

    /**
     * Searches for orders using Meilisearch. This method combines full-text search
     * with filtering capabilities while respecting pagination.
     *
     * @param searchRequest Contains search terms and filter criteria
     * @param pageable      Handles pagination parameters
     * @return A page of search results
     */
    public Page<OrderSearchResult> searchOrders(OrderSearchRequest searchRequest, Pageable pageable) {
        try {
            // Build filters and create search request
            List<String> filters = buildSearchFilters(searchRequest);
            SearchRequest request = SearchRequest.builder()
                    .q(searchRequest.getSearchTerm())
                    .filter(filters.toArray(new String[0]))
                    .page(calculatePageNumber(pageable.getPageNumber()))
                    .hitsPerPage(pageable.getPageSize())
                    .build();

            // Execute search
            SearchResultPaginated result = (SearchResultPaginated) orderIndex.search(request);

            // Convert results to DTO format and return paged response
            List<OrderSearchResult> orderResults = result.getHits().stream()
                    .map(this::convertToSearchResult)
                    .collect(Collectors.toList());

            log.debug("Found {} results for search term '{}'", orderResults.size(), searchRequest.getSearchTerm());

            return new PageImpl<>(
                    orderResults,
                    pageable,
                    result.getTotalHits()
            );

        } catch (MeilisearchException e) {
            log.error("Search operation failed: {}", e.getMessage());

            throw new SearchException("Failed to execute search", e);
        }
    }


    /**
     * Indexes an order in Meilisearch.
     *
     * @param order the {@link Order} entity to be indexed in the search system
     */
    @Override
    @Async
    public void indexOrder(Order order) {
        try {
            OrderDocument document = OrderDocument.fromEntity(order);
            String jsonDocument = objectMapper.writeValueAsString(document);
            orderIndex.addDocuments(jsonDocument);
            log.debug("Successfully indexed order: {}", order.getId());
        } catch (MeilisearchException e) {
            log.error("Failed to index order with id {} : {}", order.getId(), e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order with id {} to JSON: {}", order.getId(), e.getMessage());
        }
    }

    /**
     * Updates an existing order in the search index.
     *
     * @param order the updated {@link Order} to be reflected in the search system
     */
    @Override
    @Async
    public void updateOrder(Order order) {
        try {
            OrderDocument document = OrderDocument.fromEntity(order);
            String jsonDocument = objectMapper.writeValueAsString(document);
            orderIndex.updateDocuments(jsonDocument);
            log.debug("Successfully updated order in search index: {}", order.getId());
        } catch (MeilisearchException e) {
            log.error("Failed to update order with id {} in search index: {}", order.getId(), e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize order with id {} to JSON: {}", order.getId(), e.getMessage());
        }
    }

    /**
     * Removes an order from the search index.
     *
     * @param orderId the {@link UUID} identifier of the order to be deleted from the search system
     */
    @Override
    @Async
    public void deleteOrder(UUID orderId) {
        try {
            orderIndex.deleteDocument(orderId.toString());
            log.debug("Successfully deleted order from search index: {}", orderId);
        } catch (MeilisearchException e) {
            log.error("Failed to delete order with id {} from search index: {}", orderId, e.getMessage());
        }
    }

    /**
     * Configures the Meilisearch index by updating searchable attributes, filterable attributes,
     * and ranking rules to enable efficient full-text searches.
     */
    private void configureIndex() {
        try {
            // Full-text search attributes
            orderIndex.updateSearchableAttributesSettings(Arrays.asList(
                    "customerName",
                    "description"
            ).toArray(new String[0]));

            // Filter search attributes
            orderIndex.updateFilterableAttributesSettings(List.of(
                    "createdAt"
            ).toArray(new String[0]));

            // Rankin rules
            orderIndex.updateRankingRulesSettings(Arrays.asList(
                    "words",
                    "typo",
                    "proximity",
                    "attribute",
                    "sort",
                    "exactness"
            ).toArray(new String[0]));
        } catch (MeilisearchException e) {
            log.error("Failed to configure Meilisearch index: {}", e.getMessage());
        }
    }

    /**
     * Builds Meilisearch filters based on the search criteria.
     */
    private List<String> buildSearchFilters(OrderSearchRequest searchRequest) {
        List<String> filters = new ArrayList<>();

        try {
            // Add date range filter if specified
            if (searchRequest.getDateFrom() != null) {
                filters.add("createdAt >= " + objectMapper.writeValueAsString(searchRequest.getDateFrom()));
            }
            if (searchRequest.getDateTo() != null) {
                filters.add("createdAt <= " + objectMapper.writeValueAsString(searchRequest.getDateTo()));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize date range filter: {}", e.getMessage());
        }

        return filters;
    }

    /**
     * Converts a Meilisearch document into application's search result format
     */
    private OrderSearchResult convertToSearchResult(Map<String, Object> orderData) {
        // Try to convert map to OrderDocument
        OrderDocument document = objectMapper.convertValue(orderData, OrderDocument.class);

        return OrderSearchResult.builder()
                .id(document.getId())
                .customerName(document.getCustomerName())
                .description(document.getDescription())
                .createdAt(document.getCreatedAt())
                .totalItems(document.getTotalItems())
                .build();
    }

    /**
     * Calculates the correct page number for a query request.
     *
     * @param pageNumber the original requested page number
     * @return the adjusted page number to be used in the query
     */
    private int calculatePageNumber(long pageNumber) {
        // With 0 as page value, Meilisearch would process request, but wouldn't return any documents.
        // https://www.meilisearch.com/docs/reference/api/search#page
        long correctPage = pageNumber == 0 ? DEFAULT_PAGE_NUMBER : pageNumber;
        return Math.toIntExact(correctPage);
    }

}
