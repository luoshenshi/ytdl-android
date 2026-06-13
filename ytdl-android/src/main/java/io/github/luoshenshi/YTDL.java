package io.github.luoshenshi;

import io.github.luoshenshi.internal.*;
import io.github.luoshenshi.model.VideoFormat;
import io.github.luoshenshi.model.VideoInfo;
import okhttp3.OkHttpClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.github.luoshenshi.Utils.*;
import static io.github.luoshenshi.internal.Constants.BASE_URL;

/**
 * The main entry point for the ytdl-java library optimized for Android.
 * Use {@link #builder()} to create a new instance.
 */
public class YTDL implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(YTDL.class);
    private final OkHttpClient httpClient;
    private final SignatureDecipherer decipherer;

    YTDL(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.decipherer = new SignatureDecipherer(httpClient);
    }

    public static YTDLBuilder builder() {
        return new YTDLBuilder();
    }

    /**
     * Fetches video information for the given YouTube video ID.
     */
    public CompletableFuture<VideoInfo> getVideoInfo(String videoId) {
        return getWatchHTMLPage(videoId).thenCompose(watchPage -> {
            if (watchPage == null) {
                CompletableFuture<VideoInfo> failed = new CompletableFuture<>();
                failed.completeExceptionally(new Exception("Failed to fetch watch page"));
                return failed;
            }

            JSONObject playerResponse = watchPage.optJSONObject("player_response");
            Exception playError = Utils.playError(playerResponse);
            if (playError != null) {
                CompletableFuture<VideoInfo> failed = new CompletableFuture<>();
                failed.completeExceptionally(playError);
                return failed;
            }

            return fetchAllFormats(videoId, watchPage).thenApply(formats -> {
                try {
                    JSONObject videoDetails = playerResponse.optJSONObject("videoDetails");
                    JSONObject microformat = playerResponse.optJSONObject("microformat");
                    JSONObject playerMicroformatRenderer = microformat != null ? microformat.optJSONObject("playerMicroformatRenderer") : null;

                    String html5player = watchPage.optString("html5player");

                    // Replaced .stream().toList() with Android-safe foreach loop
                    List<VideoFormat> decipheredFormats = new ArrayList<>();
                    for (VideoFormat f : formats) {
                        String url = f.url();
                        if (url != null && (url.contains("signatureCipher") || url.contains("cipher") || !url.startsWith("http"))) {
                            url = decipherer.decipher(url, html5player);
                        }
                        decipheredFormats.add(new VideoFormat(
                                f.itag(), url, f.mimeType(), f.qualityLabel(), f.bitrate(),
                                f.audioBitrate(), f.hasVideo(), f.hasAudio(), f.container(),
                                f.codecs(), f.isLive(), f.isHLS(), f.isDashMPD(), f.contentLength()
                        ));
                    }

                    return new VideoInfo(
                            videoId,
                            videoDetails != null ? videoDetails.optString("title") : "",
                            ExtractorUtils.extractAuthor(watchPage, videoDetails, playerMicroformatRenderer),
                            videoDetails != null ? videoDetails.optString("shortDescription") : "",
                            videoDetails != null ? videoDetails.optLong("viewCount", 0L) : 0L,
                            ExtractorUtils.extractLikes(watchPage),
                            BASE_URL + videoId,
                            decipheredFormats,
                            ExtractorUtils.parseThumbnails(videoDetails != null ? videoDetails.optJSONObject("thumbnail") : null),
                            playerMicroformatRenderer != null ? JsonPath.getString(playerMicroformatRenderer, "", "embed", "iframeUrl") : null,
                            playerMicroformatRenderer != null && playerMicroformatRenderer.optBoolean("isFamilySafe"),
                            extractAvailableCountries(playerMicroformatRenderer),
                            playerMicroformatRenderer != null ? playerMicroformatRenderer.optString("category") : null,
                            playerMicroformatRenderer != null ? playerMicroformatRenderer.optString("publishDate") : null,
                            playerMicroformatRenderer != null && playerMicroformatRenderer.optBoolean("isShortsEligible"),
                            ExtractorUtils.extractRelatedVideos(watchPage),
                            ExtractorUtils.extractChapters(watchPage),
                            ExtractorUtils.extractStoryboards(playerResponse)
                    );
                } catch (Exception e) {
                    log.error("Error parsing video info JSON elements", e);
                    throw new RuntimeException("Failed to map YouTube JSON metadata safely", e);
                }
            });
        });
    }

    private CompletableFuture<List<VideoFormat>> fetchAllFormats(String videoId, JSONObject watchPage) {
        List<CompletableFuture<JSONObject>> futures = new ArrayList<>();
        String html5player = watchPage.optString("html5player");
        String visitorData = getVisitorData(watchPage);

        futures.add(ClientSimulators.fetchIos(httpClient, videoId));
        futures.add(ClientSimulators.fetchAndroid(httpClient, videoId));
        futures.add(ClientSimulators.fetchAndroidVR(httpClient, videoId, html5player, visitorData));
        futures.add(ClientSimulators.fetchWebEmbedded(httpClient, videoId, html5player));
        futures.add(ClientSimulators.fetchTv(httpClient, videoId, html5player, visitorData));

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> {
            List<VideoFormat> allFormats = new ArrayList<>();

            JSONObject fallbackPlayerResponse = watchPage.optJSONObject("player_response");
            if (fallbackPlayerResponse != null) {
                allFormats.addAll(FormatParser.extractFromPlayerResponse(fallbackPlayerResponse));
            }

            for (CompletableFuture<JSONObject> future : futures) {
                try {
                    JSONObject obj = future.join();
                    if (obj != null) {
                        allFormats.addAll(FormatParser.extractFromPlayerResponse(obj));
                    }
                } catch (Exception e) {
                    log.debug("Failed to join client simulator future", e);
                }
            }

            return allFormats;
        });
    }

    private List<String> extractAvailableCountries(JSONObject playerMicroformatRenderer) {
        List<String> countries = new ArrayList<>();
        if (playerMicroformatRenderer != null) {
            org.json.JSONArray arr = playerMicroformatRenderer.optJSONArray("availableCountries");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    countries.add(arr.optString(i));
                }
            }
        }
        return countries;
    }

    private String getVisitorData(JSONObject watchPage) {
        if (watchPage == null) return null;
        String data = JsonPath.getString(watchPage, null, "player_response", "responseContext", "serviceTrackingParams", "[service=GFEEDBACK]", "params", "[key=visitor_data]", "value");
        if (data == null) {
            data = JsonPath.getString(watchPage, null, "response", "responseContext", "serviceTrackingParams", "[service=GFEEDBACK]", "params", "[key=visitor_data]", "value");
        }
        return data;
    }

    private CompletableFuture<JSONObject> getWatchHTMLPage(String id) {
        String url = BASE_URL + id + "&hl=en&has_verified=1";
        return request(httpClient, url).thenApply(body -> {
            JSONObject info = new JSONObject();
            try {
                info.put("page", "watch");

                JSONObject playerResponse;
                try {
                    playerResponse = findJSON("watch.html", "player_response", body, "ytInitialPlayerResponse\\s*=\\s*\\{", "</script>", "{");
                } catch (Exception e) {
                    playerResponse = tryParseBetween(body, "var ytInitialPlayerResponse = ", "}};", "", "}}");
                    if (playerResponse == null) {
                        playerResponse = tryParseBetween(body, "var ytInitialPlayerResponse = ", ";var", "", "");
                    }
                }
                info.put("player_response", playerResponse);

                JSONObject initialData;
                try {
                    initialData = findJSON("watch.html", "response", body, "ytInitialData\\s*=\\s*\\{", "</script>", "{");
                } catch (Exception e) {
                    initialData = tryParseBetween(body, "var ytInitialData = ", "}};", "", "}}");
                }
                info.put("response", initialData);
                info.put("html5player", getHTML5player(body));
            } catch (Exception e) {
                log.error("Failed to construct Watch HTML page JSON elements", e);
            }

            return info;
        });
    }

    private String getHTML5player(String body) {
        if (body == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<script\\s+src=\"([^\"]*player_ias/base[^\"]*)\"|\"jsUrl\":\"([^\"]*player_ias/base[^\"]*)\"");
        java.util.regex.Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return null;
    }

    @Override
    public void close() {
        if (httpClient != null) {
            try {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
            } catch (Exception e) {
                log.error("Error closing OkHttpClient resources", e);
            }
        }
    }
}