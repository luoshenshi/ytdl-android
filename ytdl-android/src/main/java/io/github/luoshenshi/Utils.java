package io.github.luoshenshi;

import android.os.Build;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Utils {
    private static final OkHttpClient client = new OkHttpClient();

    public static Integer parseAbbreviatedNumber(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String sanitizedInput = input.replace(",", ".").replace(" ", "");

        Pattern pattern = Pattern.compile("([\\d,.]+)([MK]?)");
        Matcher matcher = pattern.matcher(sanitizedInput);

        if (matcher.find()) {
            String numString = matcher.group(1);
            String multi = matcher.group(2);

            try {
                double num = Double.parseDouble(Objects.requireNonNull(numString));
                if ("M".equals(multi)) {
                    return (int) Math.round(num * 1_000_000);
                } else if ("K".equals(multi)) {
                    return (int) Math.round(num * 1_000);
                } else {
                    return (int) Math.round(num);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static String findBetween(String string, String start, String end) {
        int startIndex = string.indexOf(start);
        if (startIndex == -1) return null;
        startIndex += start.length();
        int endIndex = string.indexOf(end, startIndex);
        if (endIndex == -1) return null;
        return string.substring(startIndex, endIndex);
    }

    public static String findBetween(String text) {
        String start = "var ytInitialPlayerResponse = ";
        int startPos = text.indexOf(start);
        if (startPos == -1) return "";
        int endPos = text.indexOf(";</script>", startPos + start.length());
        if (endPos == -1) return "";
        return text.substring(startPos + start.length(), endPos);
    }

    public static JSONObject parseJSON(String source, String varName, String json) throws Exception {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return new JSONObject(json);
        } catch (Exception e) {
            throw new Exception("Error parsing " + varName + " in " + source + ": " + e.getMessage());
        }
    }

    public static CompletableFuture<String> request(String url) {
        CompletableFuture<String> future = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            future = new CompletableFuture<>();
        }

        CompletableFuture<String> finalFuture = future;
        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    finalFuture.completeExceptionally(e);
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (response) {
                    if (response.isSuccessful()) {
                        assert response.body() != null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            finalFuture.complete(response.body().string());
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            finalFuture.completeExceptionally(new IOException("Request failed with status code: " + response.code()));
                        }
                    }
                }
            }
        });

        return future;
    }

    public static JSONObject tryParseBetween(String body, String left, String right, String prepend, String append) {
        try {
            String data = findBetween(body, left, right);
            assert data != null;
            if (data.isEmpty()) return null;
            return new JSONObject(prepend + data + append);
        } catch (Exception e) {
            return null;
        }
    }

    public static JSONObject findJSON(String source, String varName, String body, String left, String right, String prependJSON) throws Exception {
        String jsonStr = findBetween(body, left, right);
        assert jsonStr != null;
        if (jsonStr.isEmpty()) {
            throw new Exception("Could not find " + varName + " in " + source);
        }
        return parseJSON(source, varName, prependJSON + jsonStr);
    }
}
