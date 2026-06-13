package io.github.luoshenshi.internal;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles spoofing various YouTube clients (iOS, TV, VR, Web) to bypass
 * format restrictions and retrieve all available streams.
 */
public class ClientSimulators {
    private static final Logger log = LoggerFactory.getLogger(ClientSimulators.class);

    public static CompletableFuture<JSONObject> fetchAndroidVR(OkHttpClient client, String videoId, String html5player, String visitorData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = Constants.PLAYER_API_URL + "?prettyPrint=false&t=" + generateNonce(12) + "&id=" + videoId;

                JSONObject payload = buildBasePayload(videoId, html5player, client);
                payload.put("context", buildContext("ANDROID_VR", Constants.ANDROID_VR_CLIENT_VERSION, ctx -> {
                    ctx.put("deviceMake", "Oculus");
                    ctx.put("deviceModel", "Quest 3");
                    ctx.put("platform", "MOBILE");
                    ctx.put("osName", "Android");
                    ctx.put("osVersion", Constants.ANDROID_VR_OS_VERSION);
                    ctx.put("androidSdkVersion", "32");
                    if (visitorData != null) ctx.put("visitorData", visitorData);
                }));

                Request.Builder builder = new Request.Builder().url(url).post(RequestBody.create(payload.toString(), MediaType.parse("application/json"))).header("User-Agent", "com.google.android.apps.youtube.vr.oculus/" + Constants.ANDROID_VR_CLIENT_VERSION + " (Linux; U; Android " + Constants.ANDROID_VR_OS_VERSION + "; eureka-user Build/SQ3A.220605.009.A1) gzip").header("X-Goog-Api-Format-Version", "2");

                if (visitorData != null) builder.header("X-Goog-Visitor-Id", visitorData);

                return executeCall(client, builder.build());
            } catch (Exception e) {
                log.debug("Failed to fetch Android VR player: {}", e.getMessage());
                return new JSONObject();
            }
        });
    }

    public static CompletableFuture<JSONObject> fetchIos(OkHttpClient client, String videoId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("videoId", videoId);
                payload.put("cpn", generateNonce(16));
                payload.put("contentCheckOk", true);
                payload.put("racyCheckOk", true);

                payload.put("context", buildContext("IOS", Constants.IOS_CLIENT_VERSION, ctx -> {
                    ctx.put("deviceMake", "Apple");
                    ctx.put("deviceModel", Constants.IOS_DEVICE_MODEL);
                    ctx.put("platform", "MOBILE");
                    ctx.put("osName", "iOS");
                    ctx.put("osVersion", Constants.IOS_OS_VERSION);
                    ctx.put("utcOffsetMinutes", -240);
                }));

                Request request = new Request.Builder().url(Constants.PLAYER_API_URL + "?prettyPrint=false&t=" + generateNonce(12) + "&id=" + videoId).post(RequestBody.create(payload.toString(), MediaType.parse("application/json"))).header("User-Agent", "com.google.ios.youtube/" + Constants.IOS_CLIENT_VERSION + " (" + Constants.IOS_DEVICE_MODEL + "; U; CPU iOS " + Constants.IOS_USER_AGENT_VERSION + " like Mac OS X; en_US)").header("X-Goog-Api-Format-Version", "2").header("X-Youtube-Client-Name", "5").header("X-Youtube-Client-Version", Constants.IOS_CLIENT_VERSION).build();

                return executeCall(client, request);
            } catch (Exception e) {
                log.debug("Failed to fetch iOS player: {}", e.getMessage());
                return new JSONObject();
            }
        });
    }

    public static CompletableFuture<JSONObject> fetchAndroid(OkHttpClient client, String videoId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("videoId", videoId);
                payload.put("cpn", generateNonce(16));
                payload.put("contentCheckOk", true);
                payload.put("racyCheckOk", true);

                payload.put("context", buildContext("ANDROID", Constants.ANDROID_CLIENT_VERSION, ctx -> {
                    ctx.put("platform", "MOBILE");
                    ctx.put("osName", "Android");
                    ctx.put("osVersion", Constants.ANDROID_OS_VERSION);
                    ctx.put("androidSdkVersion", Constants.ANDROID_SDK_VERSION);
                    ctx.put("utcOffsetMinutes", -240);
                }));

                Request request = new Request.Builder().url(Constants.PLAYER_API_URL + "?prettyPrint=false&t=" + generateNonce(12) + "&id=" + videoId).post(RequestBody.create(payload.toString(), MediaType.parse("application/json"))).header("User-Agent", "com.google.android.youtube/" + Constants.ANDROID_CLIENT_VERSION + " (Linux; U; Android " + Constants.ANDROID_OS_VERSION + ") gzip").header("X-Goog-Api-Format-Version", "2").build();

                return executeCall(client, request);
            } catch (Exception e) {
                log.debug("Failed to fetch Android player: {}", e.getMessage());
                return new JSONObject();
            }
        });
    }

    public static CompletableFuture<JSONObject> fetchWebEmbedded(OkHttpClient client, String videoId, String html5player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = buildBasePayload(videoId, html5player, client);
                payload.put("context", buildContext("WEB_EMBEDDED_PLAYER", Constants.WEB_EMBED_CLIENT_VERSION, ctx -> {
                    ctx.put("hl", "en");
                    ctx.put("timeZone", "UTC");
                    ctx.put("utcOffsetMinutes", 0);
                }));

                Request request = new Request.Builder().url(Constants.PLAYER_API_URL + "?prettyPrint=false&t=" + generateNonce(12) + "&id=" + videoId).post(RequestBody.create(payload.toString(), MediaType.parse("application/json"))).header("X-Goog-Api-Format-Version", "2").build();

                return executeCall(client, request);
            } catch (Exception e) {
                log.debug("Failed to fetch Web Embedded player: {}", e.getMessage());
                return new JSONObject();
            }
        });
    }

    public static CompletableFuture<JSONObject> fetchTv(OkHttpClient client, String videoId, String html5player, String visitorData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject payload = buildBasePayload(videoId, html5player, client);
                payload.put("context", buildContext("TVHTML5", Constants.TVHTML5_CLIENT_VERSION, ctx -> {
                    ctx.put("hl", "en");
                    ctx.put("timeZone", "UTC");
                    ctx.put("utcOffsetMinutes", 0);
                }));

                Request.Builder builder = new Request.Builder().url(Constants.PLAYER_API_URL + "?prettyPrint=false&t=" + generateNonce(12) + "&id=" + videoId).post(RequestBody.create(payload.toString(), MediaType.parse("application/json"))).header("X-Goog-Api-Format-Version", "2");

                if (visitorData != null) builder.header("X-Goog-Visitor-Id", visitorData);

                return executeCall(client, builder.build());
            } catch (Exception e) {
                log.debug("Failed to fetch TV player: {}", e.getMessage());
                return new JSONObject();
            }
        });
    }

    private static JSONObject buildBasePayload(String videoId, String html5player, OkHttpClient client) throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("videoId", videoId);
        payload.put("contentCheckOk", true);
        payload.put("racyCheckOk", true);

        JSONObject playbackContext = new JSONObject();
        playbackContext.put("html5Preference", "HTML5_PREF_WANTS");

        String sts = fetchSignatureTimestamp(client, html5player);
        if (sts != null) {
            playbackContext.put("signatureTimestamp", Integer.parseInt(sts));
        }

        payload.put("playbackContext", new JSONObject().put("contentPlaybackContext", playbackContext));

        return payload;
    }

    private static String fetchSignatureTimestamp(OkHttpClient client, String html5player) {
        if (html5player == null || html5player.isEmpty()) return null;
        try {
            String url = html5player.startsWith("http") ? html5player : "https://www.youtube.com" + html5player;
            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    Matcher matcher = Pattern.compile("(signatureTimestamp|sts):(\\d+)").matcher(body);
                    if (matcher.find()) return matcher.group(2);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to fetch signature timestamp: {}", e.getMessage());
        }
        return null;
    }

    private static JSONObject buildContext(String clientName, String version, ContextModifier modifier) throws JSONException {
        JSONObject context = new JSONObject();
        JSONObject clientObj = new JSONObject();
        clientObj.put("clientName", clientName);
        clientObj.put("clientVersion", version);
        clientObj.put("hl", "en");
        clientObj.put("gl", "US");
        clientObj.put("utcOffsetMinutes", 0);
        modifier.modify(clientObj);

        context.put("client", clientObj);

        JSONObject request = new JSONObject();
        request.put("internalExperimentFlags", new JSONArray());
        request.put("useSsl", true);
        context.put("request", request);

        context.put("user", new JSONObject().put("lockedSafetyMode", false));
        return context;
    }

    private static JSONObject executeCall(OkHttpClient client, Request request) throws IOException, JSONException {
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Unexpected code " + response.code() + " for " + request.url());
            }
            return new JSONObject(response.body().string());
        }
    }

    private static String generateNonce(int length) {
        String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        StringBuilder nonce = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) nonce.append(charset.charAt(random.nextInt(charset.length())));
        return nonce.toString();
    }

    private interface ContextModifier {
        void modify(JSONObject clientObj) throws JSONException;
    }
}
