package io.github.luoshenshi.internal;

import io.github.luoshenshi.model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractorUtils {
    private static final Logger log = LoggerFactory.getLogger(ExtractorUtils.class);

    public static Author extractAuthor(JSONObject watchPage, JSONObject videoDetails, JSONObject microformat) {
        String defaultChannelId = videoDetails != null ? videoDetails.optString("channelId", "") : "";
        String ownerChannelName = videoDetails != null ? videoDetails.optString("author", "") : "";

        String channelId = microformat != null ? microformat.optString("channelId", defaultChannelId) : defaultChannelId;
        String name = microformat != null ? microformat.optString("ownerChannelName", ownerChannelName) : ownerChannelName;
        String ownerProfileUrl = microformat != null ? microformat.optString("ownerProfileUrl", "") : "";
        String externalChannelId = microformat != null ? microformat.optString("externalChannelId", "") : "";

        String user = null;
        if (!ownerProfileUrl.isEmpty()) {
            String[] parts = ownerProfileUrl.split("/");
            user = parts[parts.length - 1];
        }

        List<Thumbnail> thumbnails = new ArrayList<>();
        boolean isVerified = false;
        Integer subCount = null;

        try {
            Optional<JSONArray> contents = JsonPath.getArray(watchPage, "response", "contents", "twoColumnWatchNextResults", "results", "results", "contents");

            if (contents.isPresent()) {
                for (int i = 0; i < contents.get().length(); i++) {
                    JSONObject item = contents.get().optJSONObject(i);
                    if (item != null && item.has("videoSecondaryInfoRenderer")) {
                        JSONObject secondary = item.getJSONObject("videoSecondaryInfoRenderer");
                        JSONObject owner = JsonPath.getObject(secondary, "owner", "videoOwnerRenderer").orElse(null);

                        if (owner != null) {
                            thumbnails = parseThumbnails(owner.optJSONObject("thumbnail"));
                            isVerified = checkVerifiedStatus(owner.optJSONArray("badges"));
                            subCount = parseAbbreviatedNumber(JsonPath.getString(owner, null, "subscriberCountText", "simpleText"));
                            if (subCount == null) {
                                subCount = parseAbbreviatedNumber(JsonPath.getString(owner, null, "subscriberCountText", "runs", "[0]", "text"));
                            }
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract rich author info: {}", e.getMessage());
        }

        return new Author(channelId, name, user, "https://www.youtube.com/channel/" + channelId, externalChannelId.isEmpty() ? "" : "https://www.youtube.com/channel/" + externalChannelId, ownerProfileUrl, thumbnails, isVerified, subCount);
    }

    public static Long extractLikes(JSONObject watchPage) {
        try {
            Optional<JSONArray> contents = JsonPath.getArray(watchPage, "response", "contents", "twoColumnWatchNextResults", "results", "results", "contents");
            if (contents.isEmpty()) return 0L;

            for (int i = 0; i < contents.get().length(); i++) {
                JSONObject item = contents.get().optJSONObject(i);
                if (item != null && item.has("videoPrimaryInfoRenderer")) {
                    JSONArray buttons = JsonPath.getArray(item, "videoPrimaryInfoRenderer", "videoActions", "menuRenderer", "topLevelButtons").orElse(new JSONArray());

                    for (int j = 0; j < buttons.length(); j++) {
                        JSONObject button = buttons.optJSONObject(j);
                        if (button != null && button.has("segmentedLikeDislikeButtonViewModel")) {
                            String accessibilityText = JsonPath.getString(button, "", "segmentedLikeDislikeButtonViewModel", "likeButtonViewModel", "likeButtonViewModel", "toggleButtonViewModel", "toggleButtonViewModel", "defaultButtonViewModel", "buttonViewModel", "accessibilityText");

                            Pattern pattern = Pattern.compile("[\\d,.]+");
                            Matcher matcher = pattern.matcher(accessibilityText);
                            if (matcher.find()) {
                                String clean = matcher.group(0).replaceAll("\\D", "");
                                return clean.isEmpty() ? 0L : Long.parseLong(clean);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract likes: {}", e.getMessage());
        }
        return 0L;
    }

    public static List<RelatedVideo> extractRelatedVideos(JSONObject watchPage) {
        List<RelatedVideo> list = new ArrayList<>();
        try {
            Optional<JSONArray> secondaryResults = JsonPath.getArray(watchPage, "response", "contents", "twoColumnWatchNextResults", "secondaryResults", "secondaryResults", "results");
            if (secondaryResults.isEmpty()) return list;

            for (int i = 0; i < secondaryResults.get().length(); i++) {
                JSONObject item = secondaryResults.get().optJSONObject(i);
                if (item == null) continue;

                JSONObject compactVideo = item.optJSONObject("compactVideoRenderer");
                if (compactVideo != null) {
                    list.add(parseRelatedVideo(compactVideo));
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract related videos: {}", e.getMessage());
        }
        return list;
    }

    private static RelatedVideo parseRelatedVideo(JSONObject renderer) {
        String id = renderer.optString("videoId");
        String title = JsonPath.getString(renderer, "", "title", "simpleText");
        if (title.isEmpty()) title = JsonPath.getString(renderer, "", "title", "runs", "[0]", "text");

        String authorName = JsonPath.getString(renderer, "", "shortBylineText", "runs", "[0]", "text");
        String authorId = JsonPath.getString(renderer, "", "shortBylineText", "runs", "[0]", "navigationEndpoint", "browseEndpoint", "browseId");

        String shortViewCount = JsonPath.getString(renderer, "", "shortViewCountText", "simpleText");
        String viewCount = JsonPath.getString(renderer, "", "viewCountText", "simpleText");

        Integer length = null;
        String lengthText = JsonPath.getString(renderer, null, "lengthText", "simpleText");
        if (lengthText != null) {
            length = parseTimestamp(lengthText);
        }

        List<Thumbnail> thumbnails = parseThumbnails(renderer.optJSONObject("thumbnail"));
        boolean isLive = false;
        JSONArray badges = renderer.optJSONArray("badges");
        if (badges != null) {
            for (int i = 0; i < badges.length(); i++) {
                if ("LIVE NOW".equals(JsonPath.getString(badges.optJSONObject(i), "", "metadataBadgeRenderer", "label"))) {
                    isLive = true;
                    break;
                }
            }
        }

        return new RelatedVideo(id, title, authorName, authorId, shortViewCount, viewCount, length, thumbnails, isLive);
    }

    public static List<Chapter> extractChapters(JSONObject watchPage) {
        List<Chapter> chapters = new ArrayList<>();
        try {
            Optional<JSONArray> markersMap = JsonPath.getArray(watchPage, "response", "playerOverlays", "playerOverlayRenderer", "decoratedPlayerBarRenderer", "decoratedPlayerBarRenderer", "playerBar", "multiMarkersPlayerBarRenderer", "markersMap");
            if (markersMap.isEmpty()) return chapters;

            for (int i = 0; i < markersMap.get().length(); i++) {
                JSONObject marker = markersMap.get().optJSONObject(i);
                JSONArray chaptersArr = JsonPath.getArray(marker, "value", "chapters").orElse(null);
                if (chaptersArr != null) {
                    for (int j = 0; j < chaptersArr.length(); j++) {
                        JSONObject chapterObj = chaptersArr.optJSONObject(j).optJSONObject("chapterRenderer");
                        if (chapterObj != null) {
                            String title = JsonPath.getString(chapterObj, "", "title", "simpleText");
                            if (title.isEmpty())
                                title = JsonPath.getString(chapterObj, "", "title", "runs", "[0]", "text");
                            double start = chapterObj.optDouble("timeRangeStartMillis", 0) / 1000.0;
                            chapters.add(new Chapter(title, start));
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract chapters: {}", e.getMessage());
        }
        return chapters;
    }

    public static List<Storyboard> extractStoryboards(JSONObject playerResponse) {
        List<Storyboard> list = new ArrayList<>();
        String spec = JsonPath.getString(playerResponse, null, "storyboards", "playerStoryboardSpecRenderer", "spec");
        if (spec == null || spec.isEmpty()) return list;

        String[] parts = spec.split("\\|");
        if (parts.length < 2) return list;

        String baseUrl = parts[0];
        for (int i = 1; i < parts.length; i++) {
            String[] subParts = parts[i].split("#");
            if (subParts.length >= 8) {
                list.add(new Storyboard(baseUrl.replace("$L", String.valueOf(i - 1)), Integer.parseInt(subParts[0]), Integer.parseInt(subParts[1]), Integer.parseInt(subParts[2]), Integer.parseInt(subParts[5]), Integer.parseInt(subParts[3]), Integer.parseInt(subParts[4]), (int) Math.ceil(Double.parseDouble(subParts[2]) / (Double.parseDouble(subParts[3]) * Double.parseDouble(subParts[4])))));
            }
        }
        return list;
    }

    public static List<Thumbnail> parseThumbnails(JSONObject thumbnailData) {
        List<Thumbnail> list = new ArrayList<>();
        if (thumbnailData == null) return list;

        JSONArray arr = thumbnailData.optJSONArray("thumbnails");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject t = arr.optJSONObject(i);
                if (t != null) {
                    String url = t.optString("url");
                    if (!url.isEmpty() && !url.startsWith("http")) url = "https:" + url;
                    list.add(new Thumbnail(url, t.optInt("width", 0), t.optInt("height", 0)));
                }
            }
        }
        return list;
    }

    public static Integer parseAbbreviatedNumber(String input) {
        if (input == null || input.isEmpty()) return null;
        String sanitized = input.replace(",", ".").replace(" ", "");
        Matcher matcher = Pattern.compile("([\\d,.]+)([MK]?)").matcher(sanitized);

        if (matcher.find()) {
            try {
                double num = Double.parseDouble(matcher.group(1).replace(",", ""));
                String multi = matcher.group(2);
                if ("M".equals(multi)) return (int) Math.round(num * 1_000_000);
                if ("K".equals(multi)) return (int) Math.round(num * 1_000);
                return (int) Math.round(num);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Integer parseTimestamp(String timestamp) {
        if (timestamp == null) return null;
        String[] parts = timestamp.split(":");
        int seconds = 0;
        for (String part : parts) {
            seconds = seconds * 60 + Integer.parseInt(part);
        }
        return seconds;
    }

    private static boolean checkVerifiedStatus(JSONArray badges) {
        if (badges == null) return false;
        for (int i = 0; i < badges.length(); i++) {
            JSONObject badge = badges.optJSONObject(i);
            if (badge != null) {
                String tooltip = JsonPath.getString(badge, "", "metadataBadgeRenderer", "tooltip");
                if ("Verified".equals(tooltip)) return true;
            }
        }
        return false;
    }
}
