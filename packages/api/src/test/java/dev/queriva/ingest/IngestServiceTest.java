package dev.queriva.ingest;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.qdrant.client.grpc.Points;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ingest orchestration with mocked embed and Qdrant dependencies.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
class IngestServiceTest {

    private static final String COLLECTION = "news_radar";
    private static final String MODEL = "LaBSE";

    @Mock
    private ChunkingService chunkingService;

    @Mock
    private EmbedSidecarClient embedSidecarClient;

    @Mock
    private QdrantIngestRepository qdrantIngestRepository;

    @Mock
    private CollectionEmbeddingModelService collectionEmbeddingModelService;

    private IngestService ingestService;

    @BeforeEach
    void setUp() {
        ingestService = new IngestService(
                chunkingService,
                embedSidecarClient,
                qdrantIngestRepository,
                collectionEmbeddingModelService,
                32,
                512,
                64);
    }

    @Test
    void should_skip_existing_document_when_first_chunk_point_already_exists() {
        IngestDocumentDto document = sampleDocument("cluster-001");
        IngestRequest request = new IngestRequest(
                COLLECTION,
                MODEL,
                List.of(document),
                new ChunkingOptionsDto(true, 512, 64),
                UpsertMode.SKIP_EXISTING);

        when(qdrantIngestRepository.pointExists(COLLECTION, "cluster-001-chunk-1")).thenReturn(true);

        IngestResponse response = ingestService.ingestDocuments(request);

        assertThat(response.skipped()).isEqualTo(1);
        assertThat(response.ingested()).isZero();
        assertThat(response.chunksCreated()).isZero();
        verify(embedSidecarClient, never()).embed(any(), any());
    }

    @Test
    void should_throw_conflict_when_error_on_conflict_and_document_exists() {
        IngestDocumentDto document = sampleDocument("cluster-001");
        IngestRequest request = new IngestRequest(
                COLLECTION,
                MODEL,
                List.of(document),
                new ChunkingOptionsDto(true, 512, 64),
                UpsertMode.ERROR_ON_CONFLICT);

        when(qdrantIngestRepository.pointExists(COLLECTION, "cluster-001-chunk-1")).thenReturn(true);

        assertThatThrownBy(() -> ingestService.ingestDocuments(request))
                .isInstanceOf(DocumentConflictException.class);
    }

    @Test
    void should_delete_existing_chunks_before_overwrite_upsert() {
        IngestDocumentDto document = sampleDocument("cluster-001");
        IngestRequest request = new IngestRequest(
                COLLECTION,
                MODEL,
                List.of(document),
                new ChunkingOptionsDto(true, 512, 64),
                UpsertMode.OVERWRITE);
        Chunk chunk = new Chunk("cluster-001", "cluster-001-chunk-1", 1, "Title. body", "body");

        when(qdrantIngestRepository.pointExists(COLLECTION, "cluster-001-chunk-1")).thenReturn(true);
        when(chunkingService.chunk(any(Document.class), eq(512), eq(64))).thenReturn(List.of(chunk));
        when(embedSidecarClient.embed("Title. body", MODEL)).thenReturn(List.of(0.1f, 0.2f));

        ingestService.ingestDocuments(request);

        verify(qdrantIngestRepository).deleteByDocumentId(COLLECTION, "cluster-001");
        verify(qdrantIngestRepository).upsertPoints(eq(COLLECTION), any());
    }

    @Test
    void should_ingest_document_and_create_chunks_when_not_existing() {
        IngestDocumentDto document = sampleDocument("cluster-002");
        IngestRequest request = new IngestRequest(
                COLLECTION,
                MODEL,
                List.of(document),
                new ChunkingOptionsDto(true, 512, 64),
                UpsertMode.SKIP_EXISTING);
        Chunk chunk = new Chunk("cluster-002", "cluster-002-chunk-1", 1, "Title. body", "body");

        when(qdrantIngestRepository.pointExists(COLLECTION, "cluster-002-chunk-1")).thenReturn(false);
        when(chunkingService.chunk(any(Document.class), eq(512), eq(64))).thenReturn(List.of(chunk));
        when(embedSidecarClient.embed("Title. body", MODEL)).thenReturn(List.of(0.5f, 0.6f));

        IngestResponse response = ingestService.ingestDocuments(request);

        assertThat(response.ingested()).isEqualTo(1);
        assertThat(response.chunksCreated()).isEqualTo(1);
        assertThat(response.latencyMs()).isGreaterThanOrEqualTo(0);

        ArgumentCaptor<List<Points.PointStruct>> pointsCaptor = ArgumentCaptor.forClass(List.class);
        verify(qdrantIngestRepository).upsertPoints(eq(COLLECTION), pointsCaptor.capture());
        assertThat(pointsCaptor.getValue()).hasSize(1);
    }

    @Test
    void should_use_single_chunk_path_when_chunking_is_disabled() {
        IngestDocumentDto document = sampleDocument("cluster-003");
        IngestRequest request = new IngestRequest(
                COLLECTION,
                MODEL,
                List.of(document),
                new ChunkingOptionsDto(false, 512, 64),
                UpsertMode.SKIP_EXISTING);
        Chunk chunk = new Chunk("cluster-003", "cluster-003-chunk-1", 1, "Title. full", "full");

        when(qdrantIngestRepository.pointExists(COLLECTION, "cluster-003-chunk-1")).thenReturn(false);
        when(chunkingService.chunkWithoutSplitting(any(Document.class))).thenReturn(List.of(chunk));
        when(embedSidecarClient.embed("Title. full", MODEL)).thenReturn(List.of(0.3f));

        ingestService.ingestDocuments(request);

        verify(chunkingService).chunkWithoutSplitting(any(Document.class));
        verify(chunkingService, never()).chunk(any(Document.class), any(Integer.class), any(Integer.class));
    }

    private static IngestDocumentDto sampleDocument(String id) {
        return new IngestDocumentDto(
                id,
                "Title",
                "body",
                "prothomalo.com",
                "bn",
                "2026-06-15T08:30:00Z",
                "national",
                "https://example.com/article",
                id);
    }
}
