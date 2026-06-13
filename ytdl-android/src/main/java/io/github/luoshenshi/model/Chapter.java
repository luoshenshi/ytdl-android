package io.github.luoshenshi.model;

/**
 * Represents a video chapter marker.
 */
public record Chapter(
        String title,
        double startTimeSeconds
) {}
