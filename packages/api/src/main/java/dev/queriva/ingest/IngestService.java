package dev.queriva.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.qdrant.client.grpc.Points;

/**
 * Orchestrates chunking, embedding, and Qdrant upsert for POST /api/ingest/documents (SPEC §7.1).
 */
@Service
public class IngestService {

    private static final Logger logger = LoggerFactory.getLogger(IngestService.class);
    private static final String FIRST_CHUNK_SUFFIX = "-chunk-1";

    private final ChunkingService chunkingService;
    private final EmbedSidecarClient embedSidecarClient;
    private final QdrantIngestRepository qdrantIngestRepository;
    private final CollectionEmbeddingModelService collectionEmbeddingModelService;
    private final int batchSize;
    private final int defaultChunkSize;
    private final int defaultOverlap;

    /**
     * Creates the ingest orchestrator with pipeline dependencies.
     */
    public IngestService(
            ChunkingService chunkingService,
            EmbedSidecarClient embedSidecarClient,
            QdrantIngestRepository qdrantIngestRepository,
            CollectionEmbeddingModelService collectionEmbeddingModelService,
            @Value("${ingest.batch-size}") int batchSize,
            @Value("${ingest.default-chunk-size}") int defaultChunkSize,
            @Value("${ingest.default-overlap}") int defaultOverlap) {
        this.chunkingService = chunkingService;
        this.embedSidecarClient = embedSidecarClient;
        this.qdrantIngestRepository = qdrantIngestRepository;
        this.collectionEmbeddingModelService = collectionEmbeddingModelService;
        this.batchSize = batchSize;
        this.defaultChunkSize = defaultChunkSize;
        this.defaultOverlap = defaultOverlap;
    }

    /**
     * Ingests documents into the target collection and returns an ingest report.
     */
    public IngestResponse ingestDocuments(IngestRequest request) {
        long startNanos = System.nanoTime();

        collectionEmbeddingModelService.validateAndRecordModel(request.collection(), request.model());

        int ingested = 0;
        int chunksCreated = 0;
        int skipped = 0;
        int errors = 0;

        for (IngestDocumentDto ingestDocument : request.documents()) {
            try {
                IngestDocumentResult result = ingestSingleDocument(request, ingestDocument);
                ingested += result.ingested();
                chunksCreated += result.chunksCreated();
                skipped += result.skipped();
            } catch (DocumentConflictException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                errors++;
                logger.warn("Ingest failed for document '{}': {}", ingestDocument.id(), exception.getMessage());
            }
        }

        long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        return new IngestResponse(
                request.collection(),
                ingested,
                chunksCreated,
                skipped,
                errors,
                latencyMs);
    }

    private IngestDocumentResult ingestSingleDocument(IngestRequest request, IngestDocumentDto ingestDocument) {
        String firstChunkPointId = ingestDocument.id() + FIRST_CHUNK_SUFFIX;
        boolean documentExists = qdrantIngestRepository.pointExists(request.collection(), firstChunkPointId);

        if (documentExists && request.upsertMode() == UpsertMode.SKIP_EXISTING) {
            return new IngestDocumentResult(0, 0, 1);
        }

        if (documentExists && request.upsertMode() == UpsertMode.ERROR_ON_CONFLICT) {
            throw new DocumentConflictException(ingestDocument.id());
        }

        if (documentExists && request.upsertMode() == UpsertMode.OVERWRITE) {
            qdrantIngestRepository.deleteByDocumentId(request.collection(), ingestDocument.id());
        }

        Document document = new Document(ingestDocument.id(), ingestDocument.title(), ingestDocument.body());
        List<Chunk> chunks = resolveChunks(document, request.chunking());

        if (chunks.isEmpty()) {
            return new IngestDocumentResult(0, 0, 0);
        }

        List<Points.PointStruct> points = embedAndBuildPoints(chunks, ingestDocument, request.model());
        qdrantIngestRepository.upsertPoints(request.collection(), points);

        return new IngestDocumentResult(1, chunks.size(), 0);
    }

    private List<Chunk> resolveChunks(Document document, ChunkingOptionsDto chunkingOptions) {
        if (!chunkingOptions.enabled()) {
            return chunkingService.chunkWithoutSplitting(document);
        }

        int chunkSize = chunkingOptions.chunkSize() == 0 ? defaultChunkSize : chunkingOptions.chunkSize();
        int overlap = chunkingOptions.overlap();

        return chunkingService.chunk(document, chunkSize, overlap);
    }

    private List<Points.PointStruct> embedAndBuildPoints(
            List<Chunk> chunks,
            IngestDocumentDto ingestDocument,
            String model) {
        List<Points.PointStruct> points = new ArrayList<>();

        for (int batchStart = 0; batchStart < chunks.size(); batchStart += batchSize) {
            int batchEnd = Math.min(batchStart + batchSize, chunks.size());
            List<Chunk> batch = chunks.subList(batchStart, batchEnd);

            for (Chunk chunk : batch) {
                List<Float> vector = embedSidecarClient.embed(chunk.embedInput(), model);
                points.add(qdrantIngestRepository.buildPoint(chunk, ingestDocument, vector));
            }
        }

        return points;
    }

    private record IngestDocumentResult(int ingested, int chunksCreated, int skipped) {
    }
}
