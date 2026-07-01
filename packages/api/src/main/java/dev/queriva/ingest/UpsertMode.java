package dev.queriva.ingest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Document upsert behaviour for POST /api/ingest/documents (SPEC §7.5).
 */
public enum UpsertMode {
    SKIP_EXISTING("skip_existing"),
    OVERWRITE("overwrite"),
    ERROR_ON_CONFLICT("error_on_conflict");

    private final String wireValue;

    UpsertMode(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the JSON value used in API requests and responses.
     */
    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    /**
     * Parses a wire-format upsert mode string from an ingest request.
     */
    @JsonCreator
    public static UpsertMode fromWireValue(String wireValue) {
        for (UpsertMode mode : values()) {
            if (mode.wireValue.equals(wireValue)) {
                return mode;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported upsert_mode '" + wireValue + "'. "
                        + "Use one of: skip_existing, overwrite, error_on_conflict.");
    }
}
