package dev.queriva.ingest;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for logical-to-Qdrant point id mapping.
 */
@Tag("unit")
class QuerivaPointIdMapperTest {

    @Test
    void should_map_logical_point_id_to_deterministic_uuid() {
        UUID expected = UUID.nameUUIDFromBytes("cluster-001-chunk-1".getBytes());

        assertThat(QuerivaPointIdMapper.toQdrantPointId("cluster-001-chunk-1").getUuid())
                .isEqualTo(expected.toString());
    }

    @Test
    void should_map_metadata_point_id_to_deterministic_uuid() {
        UUID expected = UUID.nameUUIDFromBytes(IngestConstants.COLLECTION_METADATA_POINT_ID.getBytes());

        assertThat(QuerivaPointIdMapper.toQdrantPointId(IngestConstants.COLLECTION_METADATA_POINT_ID).getUuid())
                .isEqualTo(expected.toString());
    }
}
