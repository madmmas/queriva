package dev.queriva.health;

/**
 * Reports whether an upstream dependency responded successfully to a health probe.
 */
public enum DependencyStatus {
    CONNECTED("connected"),
    DISCONNECTED("disconnected");

    private final String label;

    DependencyStatus(String label) {
        this.label = label;
    }

    /**
     * Returns the JSON label for this status (SPEC §6).
     */
    public String label() {
        return label;
    }
}
