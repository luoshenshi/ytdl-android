package io.github.luoshenshi.model;

import java.util.List;

/**
 * Represents a YouTube channel/author.
 */
public record Author(
        String id,
        String name,
        String user,
        String channelUrl,
        String externalChannelUrl,
        String userUrl,
        List<Thumbnail> thumbnails,
        boolean isVerified,
        Integer subscriberCount
) {}
