package io.github.luoshenshi.internal;

import io.github.luoshenshi.model.VideoFormat;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FormatParser {

    public static List<VideoFormat> extractFromPlayerResponse(JSONObject playerResponse) {
        List<VideoFormat> formats = new ArrayList<>();
        if (playerResponse == null) return formats;

        JSONObject streamingData = playerResponse.optJSONObject("streamingData");
        if (streamingData == null) return formats;

        parseArray(streamingData.optJSONArray("formats"), formats);
        parseArray(streamingData.optJSONArray("adaptiveFormats"), formats);

        return formats;
    }

    private static void parseArray(JSONArray array, List<VideoFormat> formats) {
        if (array == null) return;
        for (int i = 0; i < array.length(); i++) {
            JSONObject raw = array.optJSONObject(i);
            if (raw != null) {
                formats.add(mapToRecord(raw));
            }
        }
    }

    private static VideoFormat mapToRecord(JSONObject raw) {
        int itag = raw.optInt("itag", 0);
        String url = raw.optString("url", "");

        if (url.isEmpty()) {
            String signatureCipher = raw.optString("signatureCipher", "");
            if (signatureCipher.isEmpty()) {
                signatureCipher = raw.optString("cipher", "");
            }
            if (!signatureCipher.isEmpty()) {
                url = signatureCipher;
            }
        }

        String mimeType = raw.optString("mimeType", "");
        String qualityLabel = raw.optString("qualityLabel", null);

        Integer bitrate = raw.has("bitrate") ? raw.optInt("bitrate") : null;
        Integer audioBitrate = raw.has("audioBitrate") ? raw.optInt("audioBitrate") : null;
        Long contentLength = raw.has("contentLength") ? raw.optLong("contentLength") : null;

        boolean hasVideo = qualityLabel != null && !qualityLabel.isEmpty();
        boolean hasAudio = raw.has("audioBitrate") || raw.has("audioSampleRate") || mimeType.contains("audio");

        String container = "";
        String codecs = "";
        if (mimeType.contains(";")) {
            String[] parts = mimeType.split(";");
            if (parts[0].contains("/")) container = parts[0].split("/")[1];
            if (parts.length > 1 && parts[1].contains("codecs=\"")) {
                codecs = extractBetween(parts[1], "codecs=\"", "\"");
            }
        }

        boolean isLive = url.contains("yt_live_broadcast") || url.contains("/source/yt_live_broadcast/");
        boolean isHls = url.contains("/manifest/hls_");
        boolean isDash = url.contains("/manifest/dash/");

        return new VideoFormat(itag, url, mimeType, qualityLabel, bitrate, audioBitrate, hasVideo, hasAudio, container, codecs, isLive, isHls, isDash, contentLength);
    }

    private static String extractBetween(String source, String left, String right) {
        int start = source.indexOf(left);
        if (start == -1) return "";
        start += left.length();
        int end = source.indexOf(right, start);
        if (end == -1) return "";
        return source.substring(start, end);
    }
}
