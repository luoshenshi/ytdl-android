package io.github.luoshenshi.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a single streamable format (adaptive or muxed) extracted from the player.
 */
public record VideoFormat(
        int itag,
        String url,
        String mimeType,
        String qualityLabel,
        Integer bitrate,
        Integer audioBitrate,
        boolean hasVideo,
        boolean hasAudio,
        String container,
        String codecs,
        boolean isLive,
        boolean isHLS,
        boolean isDashMPD,
        Long contentLength
) {

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("itag", this.itag);
        obj.put("url", this.url);
        obj.put("mimeType", this.mimeType);
        obj.put("qualityLabel", this.qualityLabel != null ? this.qualityLabel : JSONObject.NULL);
        obj.put("bitrate", this.bitrate != null ? this.bitrate : JSONObject.NULL);
        obj.put("audioBitrate", this.audioBitrate != null ? this.audioBitrate : JSONObject.NULL);
        obj.put("hasVideo", this.hasVideo);
        obj.put("hasAudio", this.hasAudio);
        obj.put("container", this.container);
        obj.put("codecs", this.codecs);
        obj.put("isLive", this.isLive);
        obj.put("isHLS", this.isHLS);
        obj.put("isDashMPD", this.isDashMPD);
        obj.put("contentLength", this.contentLength != null ? this.contentLength : JSONObject.NULL);
        return obj;
    }
}
