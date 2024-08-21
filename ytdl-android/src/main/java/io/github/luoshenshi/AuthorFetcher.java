package io.github.luoshenshi;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AuthorFetcher {
    private static boolean isVerified(JSONArray badges) throws JSONException {
        if (badges == null) return false;
        for (int i = 0; i < badges.length(); i++) {
            JSONObject badge = badges.getJSONObject(i);
            if ("Verified".equals(Objects.requireNonNull(badge.optJSONObject("metadataBadgeRenderer")).optString("tooltip"))) {
                return true;
            }
        }
        return false;
    }

    public static JSONObject getAuthor(JSONObject info) {
        JSONObject author = new JSONObject();
        try {
            // Retrieving the results
            JSONArray results = info.getJSONObject("response")
                    .getJSONObject("contents")
                    .getJSONObject("twoColumnWatchNextResults")
                    .getJSONObject("results")
                    .getJSONObject("results")
                    .getJSONArray("contents");

            // Finding the videoSecondaryInfoRenderer
            JSONObject v = null;
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                if (item.has("videoSecondaryInfoRenderer")) {
                    v = item;
                    break;
                }
            }
            if (v == null) return author;

            // Extracting videoOwnerRenderer
            JSONObject videoOwnerRenderer = v.getJSONObject("videoSecondaryInfoRenderer")
                    .getJSONObject("owner")
                    .getJSONObject("videoOwnerRenderer");

            String channelId = videoOwnerRenderer.getJSONObject("navigationEndpoint")
                    .getJSONObject("browseEndpoint")
                    .getString("browseId");

            // Processing thumbnails
            List<JSONObject> thumbnails = new ArrayList<>();
            JSONObject thumbnailObject = videoOwnerRenderer.getJSONObject("thumbnail");
            JSONArray thumbnailArray = thumbnailObject.getJSONArray("thumbnails");
//            System.out.println(thumbnailObject);
            for (int i = 0; i < thumbnailArray.length(); i++) {
                JSONObject thumbnail = thumbnailArray.getJSONObject(i);
                try {
                    String url = new URL(thumbnail.getString("url")).toString();
                    thumbnail.put("url", url);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(e.getLocalizedMessage());
                }
                thumbnails.add(thumbnail);
            }

            // Extracting subscriber count and verify status
            String subscriberObjectString = Objects.requireNonNull(videoOwnerRenderer.optJSONObject("subscriberCountText")).toString();
            JSONObject subscriberObject = new JSONObject(subscriberObjectString);
            String subscriberCount = subscriberObject.getString("simpleText").replace("subscribers", "").trim(); // like 1M, 2M~~

            boolean verified = isVerified(videoOwnerRenderer.optJSONArray("badges"));

            // Extracting video details
            JSONObject videoDetails = Objects.requireNonNull(Objects.requireNonNull(info.optJSONObject("player_response"))
                            .optJSONObject("microformat"))
                    .optJSONObject("playerMicroformatRenderer");

            String id = (videoDetails != null ? videoDetails.optString("channelId", channelId) : channelId);

            // Creating the author object
            author.put("id", id);
            author.put("name", videoDetails != null ? videoDetails.optString("ownerChannelName") :
                    info.getJSONObject("player_response").getJSONObject("videoDetails").optString("author"));
            author.put("user", videoDetails != null ? videoDetails.optString("ownerProfileUrl").split("/")[3] : null);
            author.put("channel_url", "https://www.youtube.com/channel/" + id);
            author.put("external_channel_url", videoDetails != null ? "https://www.youtube.com/channel/" + videoDetails.optString("externalChannelId") : "");
            author.put("user_url", videoDetails != null ? new URL(videoDetails.optString("ownerProfileUrl")).toString() : "");
            author.put("thumbnails", thumbnails);
            author.put("verified", verified);
            author.put("subscriber_count", Utils.parseAbbreviatedNumber(subscriberCount));
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getLocalizedMessage());
        }
        return author;
    }
}
