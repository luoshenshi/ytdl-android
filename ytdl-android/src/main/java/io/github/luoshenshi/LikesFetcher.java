package io.github.luoshenshi;

import org.json.JSONArray;
import org.json.JSONObject;

public class LikesFetcher {
    public static String getLikes(JSONObject info) {
        try {
            // Retrieving the results
            JSONObject contentsObj = info.getJSONObject("response")
                    .getJSONObject("contents")
                    .getJSONObject("twoColumnWatchNextResults")
                    .getJSONObject("results")
                    .getJSONObject("results");
            JSONArray contents = contentsObj.getJSONArray("contents");

            // Getting videoPrimaryInfoRenderer;
            JSONObject video = null;
            for (int i = 0; i < contents.length(); i++) {
                JSONObject item = contents.getJSONObject(i);
                if (item.has("videoPrimaryInfoRenderer")) {
                    video = item;
                    break;
                }
            }

            if (video == null) {
                return null;
            }

            JSONArray buttons = video.getJSONObject("videoPrimaryInfoRenderer")
                    .getJSONObject("videoActions")
                    .getJSONObject("menuRenderer")
                    .getJSONArray("topLevelButtons");

            JSONObject likeButton = null;
            for (int i = 0; i < buttons.length(); i++) {
                JSONObject button = buttons.getJSONObject(i);
                if (button.has("segmentedLikeDislikeButtonViewModel")) {
                    likeButton = button;
                    break;
                }
            }

            if (likeButton == null) {
                return null;
            }

            String accessibilityText = likeButton.getJSONObject("segmentedLikeDislikeButtonViewModel")
                    .getJSONObject("likeButtonViewModel")
                    .getJSONObject("likeButtonViewModel")
                    .getJSONObject("toggleButtonViewModel")
                    .getJSONObject("toggleButtonViewModel")
                    .getJSONObject("defaultButtonViewModel")
                    .getJSONObject("buttonViewModel")
                    .getString("accessibilityText");

            return accessibilityText.replaceAll("\\D", "");

        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
            return null;
        }
    }
}
