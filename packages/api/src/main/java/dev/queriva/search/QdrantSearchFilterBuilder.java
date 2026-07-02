package dev.queriva.search;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.google.protobuf.Timestamp;
import dev.queriva.ingest.IngestConstants;
import io.qdrant.client.grpc.Points;

import static io.qdrant.client.ConditionFactory.datetimeRange;
import static io.qdrant.client.ConditionFactory.match;
import static io.qdrant.client.ConditionFactory.matchKeyword;

/**
 * Builds typed Qdrant filters for vector search — no string-concatenated DSL (code-quality.mdc E5).
 */
public final class QdrantSearchFilterBuilder {

    private QdrantSearchFilterBuilder() {
    }

    /**
     * Builds a Qdrant filter from optional search filters, excluding internal metadata points.
     */
    public static Points.Filter buildFilter(SearchFilters filters) {
        Points.Filter.Builder filterBuilder = Points.Filter.newBuilder()
                .addMustNot(match(IngestConstants.PAYLOAD_QUERIVA_INTERNAL, true));

        if (filters == null) {
            return filterBuilder.build();
        }

        if (filters.language() != null && !filters.language().isBlank()) {
            filterBuilder.addMust(matchKeyword(IngestConstants.PAYLOAD_LANGUAGE, filters.language().trim()));
        }

        if (filters.category() != null && !filters.category().isBlank()) {
            filterBuilder.addMust(matchKeyword(IngestConstants.PAYLOAD_CATEGORY, filters.category().trim()));
        }

        Points.DatetimeRange.Builder dateRangeBuilder = Points.DatetimeRange.newBuilder();
        boolean hasDateRange = false;

        if (filters.dateFrom() != null && !filters.dateFrom().isBlank()) {
            dateRangeBuilder.setGte(toTimestamp(filters.dateFrom().trim(), SearchConstants.DATE_FILTER_START_SUFFIX));
            hasDateRange = true;
        }

        if (filters.dateTo() != null && !filters.dateTo().isBlank()) {
            dateRangeBuilder.setLte(toTimestamp(filters.dateTo().trim(), SearchConstants.DATE_FILTER_END_SUFFIX));
            hasDateRange = true;
        }

        if (hasDateRange) {
            filterBuilder.addMust(datetimeRange(IngestConstants.PAYLOAD_PUBLISHED_AT, dateRangeBuilder.build()));
        }

        return filterBuilder.build();
    }

    private static Timestamp toTimestamp(String dateValue, String timeSuffix) {
        try {
            Instant instant = Instant.parse(dateValue.contains("T") ? dateValue : dateValue + timeSuffix);
            return Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Filter date '" + dateValue + "' is not a valid ISO-8601 date. Use YYYY-MM-DD or full timestamp.",
                    exception);
        }
    }
}
