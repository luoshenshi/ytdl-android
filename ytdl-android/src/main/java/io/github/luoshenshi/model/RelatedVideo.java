package io.github.luoshenshi.model;

import java.util.List;

/**
 * Represents a related video recommendation.
 */
public record RelatedVideo(
        String id,
        String title,
        String authorName,
        String authorId,
        String shortViewCountText,
        String viewCount,
        Integer lengthSeconds,
        List<Thumbnail> thumbnails,
        boolean isLive
) {}
