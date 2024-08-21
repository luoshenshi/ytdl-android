package io.github.luoshenshi;

import static io.github.luoshenshi.Utils.findBetween;
import static io.github.luoshenshi.Utils.findJSON;
import static io.github.luoshenshi.Utils.request;
import static io.github.luoshenshi.Utils.tryParseBetween;
import static io.github.luoshenshi.YtdlConstants.ANDROID_CLIENT_VERSION;
import static io.github.luoshenshi.YtdlConstants.ANDROID_OS_VERSION;
import static io.github.luoshenshi.YtdlConstants.ANDROID_SDK_VERSION;
import static io.github.luoshenshi.YtdlConstants.BASE_URL;
import static io.github.luoshenshi.YtdlConstants.IOS_CLIENT_VERSION;
import static io.github.luoshenshi.YtdlConstants.IOS_DEVICE_MODEL;
import static io.github.luoshenshi.YtdlConstants.IOS_OS_VERSION;
import static io.github.luoshenshi.YtdlConstants.IOS_USER_AGENT_VERSION;
import static io.github.luoshenshi.YtdlConstants.client;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class YTDL {
    private static final Gson gson = new Gson();
    private static final Cache cache = new Cache(1000);

    public static void getInfoAsync(String videoId, YtdlResponse<VideoInfo> callback) {
        new Thread(() -> {
            try {
                // Getting just basic info~
                JSONObject basicInfo = getBasicInfo(videoId);

                JSONObject microformat = getMicroformat(videoId);

                // Getting video formats of both iOS and Android...
                List<JSONObject> formats = fetchVideoFormats(videoId);

                // Extracting Thumbnails
                List<VideoInfo.Thumbnail> thumbnailList = extractThumbnails(basicInfo.optJSONObject("thumbnails"));

                // Extracting microformats
                JSONObject playerMicroformatRenderer = microformat.optJSONObject("playerMicroformatRenderer");

                String iframeUrl = playerMicroformatRenderer != null ? Objects.requireNonNull(playerMicroformatRenderer.optJSONObject("embed")).optString("iframeUrl") : null;
                String ownerProfileUrl = playerMicroformatRenderer != null ? playerMicroformatRenderer.optString("ownerProfileUrl") : null;
                String externalChannelId = playerMicroformatRenderer != null ? playerMicroformatRenderer.optString("externalChannelId") : null;
                Boolean isFamilySafe = playerMicroformatRenderer != null ? playerMicroformatRenderer.optBoolean("isFamilySafe") : null;
                List<String> availableCountries = extractAvailableCountries(playerMicroformatRenderer);
                String category = playerMicroformatRenderer != null ? playerMicroformatRenderer.optString("category") : null;
                String uploadDate = playerMicroformatRenderer != null ? playerMicroformatRenderer.optString("publishDate") : null;
                Boolean isShortsEligible = playerMicroformatRenderer != null ? playerMicroformatRenderer.optBoolean("isShortsEligible") : null;

                // Fetching likes, and author info from watch HTML page
                CompletableFuture<JSONObject> info = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    info = getWatchHTMLPage(videoId);
                }
                JSONObject watchHtmlPage = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    watchHtmlPage = info != null ? info.join() : null;
                }
                String likesCount = LikesFetcher.getLikes(watchHtmlPage);
                JSONObject authorInfo = AuthorFetcher.getAuthor(watchHtmlPage);

                // Constructing and returning VideoInfo
                VideoInfo videoInfo = new VideoInfo(
                        basicInfo.optString("title"),
                        basicInfo.optString("author"),
                        basicInfo.optString("description"),
                        basicInfo.optString("views"),
                        likesCount,
                        basicInfo.optString("video_url"),
                        formats,
                        thumbnailList,
                        iframeUrl,
                        ownerProfileUrl,
                        externalChannelId,
                        Boolean.TRUE.equals(isFamilySafe),
                        availableCountries,
                        category,
                        uploadDate,
                        Boolean.TRUE.equals(isShortsEligible),
                        authorInfo
                );
                callback.onResponse(videoInfo);
                cache.clear();
            } catch (Exception e) {
                cache.clear();
                callback.onFailure(e);
            }
        }).start();
    }

    private static JSONObject getBasicInfo(String videoId) throws IOException {
        String videoUrl = BASE_URL + videoId;

        Request request = new Request.Builder()
                .url(videoUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = client.newCall(request).execute()) {

            assert response.body() != null;

            String htmlBody = response.body().string();
            JSONObject playerResponse = parsePlayerResponse(htmlBody);
            List<JSONObject> formats = parseFormats(playerResponse);
            JSONObject videoDetails = playerResponse.getJSONObject("videoDetails");
            JSONObject result = new JSONObject();

            // returning the basic info
            result.put("title", videoDetails.getString("title"));
            result.put("author", videoDetails.getString("author"));
            result.put("description", videoDetails.getString("shortDescription"));
            result.put("views", videoDetails.getString("viewCount"));
            result.put("video_url", videoUrl);
            result.put("formats", formats);
            result.put("thumbnails", videoDetails.optJSONObject("thumbnail"));

            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject parsePlayerResponse(String htmlBody) throws JSONException {
        String playerResponseString = findBetween(htmlBody);
        return new JSONObject(playerResponseString);
    }

    private static List<JSONObject> parseFormats(JSONObject playerResponse) throws JSONException {
        List<JSONObject> formats = new ArrayList<>();
        if (playerResponse.has("streamingData")) {
            JSONObject streamingData = playerResponse.getJSONObject("streamingData");
            if (streamingData.has("formats")) {
                JSONArray formatArray = streamingData.getJSONArray("formats");
                for (int i = 0; i < formatArray.length(); i++) {
                    formats.add(formatArray.getJSONObject(i));
                }
            }
            if (streamingData.has("adaptiveFormats")) {
                JSONArray adaptiveFormatArray = streamingData.getJSONArray("adaptiveFormats");
                for (int i = 0; i < adaptiveFormatArray.length(); i++) {
                    formats.add(adaptiveFormatArray.getJSONObject(i));
                }
            }
        }
        return formats;
    }

    private static List<VideoInfo.Thumbnail> extractThumbnails(JSONObject thumbnailData) throws JSONException {
        List<VideoInfo.Thumbnail> thumbnails = new ArrayList<>();
        if (thumbnailData != null) {
            JSONArray thumbnailList = thumbnailData.optJSONArray("thumbnails");
            if (thumbnailList != null) {
                for (int i = 0; i < thumbnailList.length(); i++) {
                    JSONObject thumbnail = thumbnailList.getJSONObject(i);
                    String url = thumbnail.getString("url");
                    int width = thumbnail.getInt("width");
                    int height = thumbnail.getInt("height");
                    thumbnails.add(new VideoInfo.Thumbnail(url, width, height));
                }
            }
        }
        return thumbnails;
    }

    private static List<String> extractAvailableCountries(JSONObject playerMicroformatRenderer) {
        List<String> availableCountries = new ArrayList<>();
        JSONArray availableCountriesArray = playerMicroformatRenderer != null ? playerMicroformatRenderer.optJSONArray("availableCountries") : null;

        if (availableCountriesArray != null) {
            for (int i = 0; i < availableCountriesArray.length(); i++) {
                availableCountries.add(availableCountriesArray.optString(i));
            }
        }

        return availableCountries;
    }


    private static JSONObject getMicroformat(String videoId) throws IOException {
        String videoUrl = BASE_URL + videoId;
        Request request = new Request.Builder()
                .url(videoUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            String htmlBody = response.body().string();
            JSONObject playerResponse = parsePlayerResponse(htmlBody);
            return playerResponse.optJSONObject("microformat");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<JSONObject> fetchVideoFormats(String videoId) throws Exception {
        List<JSONObject> formats = new ArrayList<>();

        JSONObject iosPlayerResponse = fetchIosJsonPlayer(videoId);
        JSONObject androidPlayerResponse = fetchAndroidJsonPlayer(videoId);

        formats.addAll(parseFormats(androidPlayerResponse));
        formats.addAll(parseFormats(iosPlayerResponse));

        // Handle additional formats like DASH and HLS
        addManifestFormats(androidPlayerResponse, formats);
        addManifestFormats(iosPlayerResponse, formats);

        return formats;
    }

    private static void addManifestFormats(JSONObject playerResponse, List<JSONObject> formats) throws IOException, SAXException, JSONException {
        if (playerResponse.has("dashManifestUrl")) {
            formats.addAll(getDashManifest(playerResponse.getString("dashManifestUrl")));
        }
        if (playerResponse.has("hlsManifestUrl")) {
            formats.addAll(getM3U8Manifest(playerResponse.getString("hlsManifestUrl")));
        }
    }

    private static String generateClientPlaybackNonce() {
        String charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder nonce = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 16; i++) {
            nonce.append(charset.charAt(random.nextInt(charset.length())));
        }
        return nonce.toString();
    }

    private static Map<String, Object> createJsonPayload(String videoId, String clientName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("videoId", videoId);
        payload.put("cpn", generateClientPlaybackNonce());
        payload.put("contentCheckOk", true);
        payload.put("racyCheckOk", true);

        Map<String, Object> context = new HashMap<>();
        Map<String, Object> client = getStringObjectMap(clientName);
        context.put("client", client);

        Map<String, Object> request = new HashMap<>();
        request.put("internalExperimentFlags", new ArrayList<>());
        request.put("useSsl", true);
        context.put("request", request);

        Map<String, Object> user = new HashMap<>();
        user.put("lockedSafetyMode", false);
        context.put("user", user);

        payload.put("context", context);
        return payload;
    }

    private static @NotNull Map<String, Object> getStringObjectMap(String clientName) {
        Map<String, Object> client = new HashMap<>();
        client.put("clientName", clientName);
        client.put("clientVersion", clientName.equals("IOS") ? IOS_CLIENT_VERSION : ANDROID_CLIENT_VERSION);
        client.put("platform", "MOBILE");
        client.put("osName", clientName.equals("IOS") ? "iOS" : "Android");
        client.put("osVersion", clientName.equals("IOS") ? IOS_OS_VERSION : ANDROID_OS_VERSION);
        if (clientName.equals("ANDROID")) {
            client.put("androidSdkVersion", ANDROID_SDK_VERSION);
        }
        client.put("hl", "en");
        client.put("gl", "US");

        client.put("utcOffsetMinutes", -240);
        return client;
    }


    private static JSONObject fetchIosJsonPlayer(String videoId) throws IOException {
        String url = "https://youtubei.googleapis.com/youtubei/v1/player";
        Map<String, Object> payload = createJsonPayload(videoId, "IOS");
        RequestBody body = RequestBody.create(gson.toJson(payload), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .header("User-Agent", "com.google.ios.youtube/" + IOS_CLIENT_VERSION + " (" + IOS_DEVICE_MODEL + "; U; CPU iOS " + IOS_USER_AGENT_VERSION + " like Mac OS X; en_US)")
                .header("X-Goog-Api-Format-Version", "2")
                .build();

        try (Response response = client.newCall(request).execute()) {
            // Converting LinkedTreeMap to JSONObject (*_*)
            assert response.body() != null;
            String jsonString = gson.toJson(gson.fromJson(response.body().string(), Map.class));
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSONObject fetchAndroidJsonPlayer(String videoId) throws IOException {
        String url = "https://youtubei.googleapis.com/youtubei/v1/player";
        Map<String, Object> payload = createJsonPayload(videoId, "ANDROID");
        RequestBody body = RequestBody.create(gson.toJson(payload), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .header("User-Agent", "com.google.android.youtube/" + ANDROID_CLIENT_VERSION + " (Linux; U; Android " + ANDROID_OS_VERSION + "; en_US) gzip")
                .header("X-Goog-Api-Format-Version", "2")
                .build();

        try (Response response = client.newCall(request).execute()) {
            // Converting LinkedTreeMap to JSONObject
            assert response.body() != null;
            String jsonString = gson.toJson(gson.fromJson(response.body().string(), Map.class));
            return new JSONObject(jsonString);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }


    private static List<JSONObject> getDashManifest(String url) throws IOException, SAXException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            InputStream inputStream = response.body().byteStream();
            DashHandler dashHandler = new DashHandler();
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, dashHandler);
            return dashHandler.getFormats();
        } catch (ParserConfigurationException e) {
            throw new IOException("Error parsing DASH manifest", e);
        }
    }

    private static List<JSONObject> getM3U8Manifest(String url) throws IOException {
        List<JSONObject> formats = new ArrayList<>();
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build();

        try (Response response = client.newCall(request).execute()) {
            assert response.body() != null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#EXT-X-STREAM-INF")) {
                    formats.add(parseM3U8Format(line));
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return formats;
    }

    private static JSONObject parseM3U8Format(String line) throws JSONException {
        Map<String, String> attributes = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            attributes = Arrays.stream(line.substring("#EXT-X-STREAM-INF:".length()).split(","))
                    .map(attr -> attr.split("="))
                    .collect(Collectors.toMap(attr -> attr[0], attr -> attr[1]));
        }

        JSONObject format = new JSONObject();
        assert attributes != null;
        format.put("url", attributes.get("URI"));
        format.put("bandwidth", attributes.get("BANDWIDTH"));
        format.put("resolution", attributes.get("RESOLUTION"));
        return format;
    }

    private static String getWatchHTMLURL(String id) {
        String lang = "en";
        long bpctr = System.currentTimeMillis() / 1000;
        return BASE_URL + id + "&hl=" + lang + "&bpctr=" + bpctr + "&has_verified=1";
    }

    private static CompletableFuture<String> getWatchHTMLPageBody(String id) throws Exception {
        String url = getWatchHTMLURL(id);
        return cache.getOrSet(url, () -> request(url));
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static CompletableFuture<JSONObject> getWatchHTMLPage(String id) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return getWatchHTMLPageBody(id).thenApply(body -> {
                    JSONObject info = new JSONObject();
                    try {
                        info.put("page", "watch");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        // Trying to parse playerResponse
                        JSONObject playerResponse = tryParseBetween(body, "var ytInitialPlayerResponse = ", "}};", "", "}}");
                        if (playerResponse == null) {
                            playerResponse = tryParseBetween(body, "var ytInitialPlayerResponse = ", ";var", "", "");
                        }
                        if (playerResponse == null) {
                            playerResponse = tryParseBetween(body, "var ytInitialPlayerResponse = ", ";</script>", "", "");
                        }
                        if (playerResponse == null) {
                            playerResponse = findJSON("watch.html", "player_response", body, "ytInitialPlayerResponse\\s*=\\s*\\{", "</script>", "{");
                        }
                        info.put("player_response", playerResponse);

                        // Trying to parse response
                        JSONObject response = tryParseBetween(body, "var ytInitialData = ", "}};", "", "}}");
                        if (response == null) {
                            response = tryParseBetween(body, "var ytInitialData = ", ";</script>", "", "");
                        }
                        if (response == null) {
                            response = tryParseBetween(body, "window[\"ytInitialData\"] = ", "}};", "", "}}");
                        }
                        if (response == null) {
                            response = tryParseBetween(body, "window[\"ytInitialData\"] = ", ";</script>", "", "");
                        }
                        if (response == null) {
                            response = findJSON("watch.html", "response", body, "ytInitialData\\s*=\\s*\\{", "</script>", "{");
                        }
                        info.put("response", response);

                        // Getting HTML5 player
                        info.put("html5player", getHTML5player(body));
                    } catch (Exception e) {
                        cache.clear();
                        throw new RuntimeException("Error when parsing watch.html, maybe YouTube made a change.\n" +
                                "Please report this issue with the \"watch.html\" file on https://github.com/luoshenshi/ytdl-java/issues.", e);
                    }
                    return info;
                });
            }
        } catch (Exception e) {
            CompletableFuture<JSONObject> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
        return null;
    }

    private static class DashHandler extends DefaultHandler {
        private final List<JSONObject> formats = new ArrayList<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (qName.equals("Representation")) {
                JSONObject format = new JSONObject();
                try {
                    format.put("bandwidth", attributes.getValue("bandwidth"));
                    format.put("width", attributes.getValue("width"));
                    format.put("height", attributes.getValue("height"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                formats.add(format);
            }
        }

        private List<JSONObject> getFormats() {
            return formats;
        }
    }

    private static String getHTML5player(String body) {
        String regex = "<script\\s+src=\"([^\"]*player_ias/base[^\"]*)\"\\s*(?:type=\"text/javascript\")?\\s*name=\"player_ias/base\"\\s*>|\"jsUrl\":\"([^\"]*player_ias/base[^\"]*)\"";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(body);

        if (matcher.find()) {
            String scriptSrc = matcher.group(1);
            if (scriptSrc != null && !scriptSrc.isEmpty()) {
                return scriptSrc;
            }

            String jsUrl = matcher.group(2);
            if (jsUrl != null && !jsUrl.isEmpty()) {
                return jsUrl;
            }
        }
        cache.clear();
        return null;
    }
}
