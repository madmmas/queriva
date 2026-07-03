package dev.queriva.search;

/**
 * Named constants for RAG prompt and Ollama synthesis (code-quality.mdc A2).
 */
public final class RagSynthesisConstants {

    /** Ollama generate endpoint path (SPEC §8 step 4b). */
    public static final String OLLAMA_GENERATE_PATH = "/api/generate";

    /** Ollama requests use non-streaming mode — await full completion (ADR-006). */
    public static final boolean OLLAMA_STREAM_DISABLED = false;

    /** System instruction prefix per SPEC §10. */
    public static final String SYSTEM_PROMPT = """
            You are a search assistant. Answer the user's question using ONLY \
            the provided articles. Cite article titles inline. Be concise. \
            If the articles don't contain the answer, say so clearly.""";

    /** Label prefix for numbered articles in the RAG prompt. */
    public static final String ARTICLES_SECTION_HEADER = "Articles:";

    /** Label prefix for the user question in the RAG prompt. */
    public static final String USER_QUESTION_PREFIX = "User question: ";

    private RagSynthesisConstants() {
    }
}
