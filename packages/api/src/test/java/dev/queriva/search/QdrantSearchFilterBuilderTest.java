package dev.queriva.search;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.qdrant.client.grpc.Points;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for typed Qdrant search filter construction.
 */
@Tag("unit")
class QdrantSearchFilterBuilderTest {

    @Test
    void should_exclude_internal_metadata_points_when_filters_are_null() {
        Points.Filter filter = QdrantSearchFilterBuilder.buildFilter(null);

        assertThat(filter.getMustNotList()).hasSize(1);
        assertThat(filter.getMustList()).isEmpty();
    }

    @Test
    void should_add_language_category_and_date_range_conditions() {
        SearchFilters filters = new SearchFilters("bn", "2026-06-15", "2026-06-17", "national");

        Points.Filter filter = QdrantSearchFilterBuilder.buildFilter(filters);

        assertThat(filter.getMustList()).hasSize(3);
        assertThat(filter.getMustNotList()).hasSize(1);
    }

    @Test
    void should_reject_invalid_date_filter_values() {
        SearchFilters filters = new SearchFilters(null, "not-a-date", null, null);

        assertThatThrownBy(() -> QdrantSearchFilterBuilder.buildFilter(filters))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a valid ISO-8601 date");
    }
}
