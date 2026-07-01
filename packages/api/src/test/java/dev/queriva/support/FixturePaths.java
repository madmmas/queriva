package dev.queriva.support;

import java.nio.file.Path;

/**
 * Resolves monorepo fixture paths when API tests run from {@code packages/api}.
 */
public final class FixturePaths {

    private static final String FIXTURES_DIRECTORY = "fixtures";

    private FixturePaths() {
    }

    /**
     * Returns the path to a shared fixture file at the repository root.
     */
    public static Path repoFixture(String fileName) {
        return Path.of("..", "..", FIXTURES_DIRECTORY, fileName).toAbsolutePath().normalize();
    }
}
