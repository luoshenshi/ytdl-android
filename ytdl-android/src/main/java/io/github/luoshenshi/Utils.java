package io.github.luoshenshi;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String between(String haystack, String left, String right) {
        int pos = haystack.indexOf(left);
        if (pos == -1) return "";
        pos += left.length();
        String substring = haystack.substring(pos);
        int endPos = substring.indexOf(right);
        if (endPos == -1) return "";
        return substring.substring(0, endPos);
    }

    public static String betweenRegex(String haystack, String leftRegex, String right) {
        Pattern pattern = Pattern.compile(leftRegex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(haystack);
        if (!matcher.find()) return "";
        int pos = matcher.end();
        String substring = haystack.substring(pos);
        int endPos = substring.indexOf(right);
        if (endPos == -1) return "";
        return substring.substring(0, endPos);
    }

    public static String cutAfterJS(String mixedJson) throws Exception {
        char open, close;
        if (mixedJson.charAt(0) == '[') {
            open = '[';
            close = ']';
        } else if (mixedJson.charAt(0) == '{') {
            open = '{';
            close = '}';
        } else {
            throw new Exception("Can't cut unsupported JSON: " + mixedJson.charAt(0));
        }

        int counter = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < mixedJson.length(); i++) {
            char c = mixedJson.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == open) counter++;
                else if (c == close) counter--;

                if (counter == 0) return mixedJson.substring(0, i + 1);
            }
        }
        throw new Exception("No matching closing bracket found");
    }

    public static Exception playError(JSONObject playerResponse) {
        if (playerResponse == null) return null;
        JSONObject playability = playerResponse.optJSONObject("playabilityStatus");
        if (playability == null) return null;

        String status = playability.optString("status");
        if ("OK".equals(status)) return null;

        String reason = playability.optString("reason", "Unknown error");
        return new Exception(reason);
    }

    public static CompletableFuture<String> request(OkHttpClient client, String url) {
        Request request = new Request.Builder().url(url).build();
        CompletableFuture<String> future = new CompletableFuture<>();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (response) {
                    if (response.isSuccessful() && response.body() != null) {
                        future.complete(response.body().string());
                    } else {
                        future.completeExceptionally(new IOException("Status code: " + response.code()));
                    }
                }
            }
        });
        return future;
    }

    public static JSONObject tryParseBetween(String body, String left, String right, String prepend, String append) {
        try {
            String data = between(body, left, right);
            if (data.isEmpty()) return null;
            return new JSONObject(prepend + data + append);
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject findJSON(String source, String varName, String body, String leftRegex, String right, String prependJSON) throws Exception {
        String jsonStr = betweenRegex(body, leftRegex, right);
        if (jsonStr.isEmpty()) throw new Exception("Could not find " + varName);
        return new JSONObject(cutAfterJS(prependJSON + jsonStr));
    }
}
