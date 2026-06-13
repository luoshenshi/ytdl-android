package io.github.luoshenshi.model;

import android.os.Build;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents the complete extracted metadata and available formats for a YouTube video.
 */
public record VideoInfo(
        String videoId,
        String title,
        Author author,
        String description,
        Long views,
        Long likes,
        String videoUrl,
        List<VideoFormat> formats,
        List<Thumbnail> thumbnails,
        String iframeUrl,
        boolean isFamilySafe,
        List<String> availableCountries,
        String category,
        String uploadDate,
        boolean isShortsEligible,
        List<RelatedVideo> relatedVideos,
        List<Chapter> chapters,
        List<Storyboard> storyboards
) {
    /**
     * Returns only the formats that contain video.
     *
     * @return a list of video formats
     */
    public List<VideoFormat> videoFormatsOnly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return formats.stream().filter(VideoFormat::hasVideo).toList();
        }

        return formats.stream().filter(VideoFormat::hasVideo).collect(Collectors.toList());
    }

    /**
     * Returns only the formats that contain audio but no video.
     *
     * @return a list of audio-only formats
     */
    public List<VideoFormat> audioFormatsOnly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return formats.stream().filter(f -> f.hasAudio() && !f.hasVideo()).toList();
        }
        return formats.stream().filter(f -> f.hasAudio() && !f.hasVideo()).collect(Collectors.toList());
    }
}
