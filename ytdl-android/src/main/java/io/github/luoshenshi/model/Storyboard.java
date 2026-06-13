package io.github.luoshenshi.model;

/**
 * Represents storyboard thumbnail metadata.
 */
public record Storyboard(
        String templateUrl,
        int thumbnailWidth,
        int thumbnailHeight,
        int thumbnailCount,
        int interval,
        int columns,
        int rows,
        int storyboardCount
) {}
