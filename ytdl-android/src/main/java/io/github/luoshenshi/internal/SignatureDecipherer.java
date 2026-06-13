package io.github.luoshenshi.internal;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles deciphering YouTube signature ciphers and transforming the 'n' parameter.
 * Ports the logic from sig.js using regex-based extraction for Java performance.
 */
public class SignatureDecipherer {
    private static final Logger log = LoggerFactory.getLogger(SignatureDecipherer.class);

    private final OkHttpClient client;
    private final Map<String, DecipherFunction> decipherCache = new HashMap<>();

    public SignatureDecipherer(OkHttpClient client) {
        this.client = client;
    }

    public String decipher(String urlOrCipher, String html5playerUrl) {
        if (urlOrCipher.startsWith("http") && !urlOrCipher.contains("signatureCipher") && !urlOrCipher.contains("cipher")) {
            return transformN(urlOrCipher, html5playerUrl);
        }

        Map<String, String> params = parseQueryString(urlOrCipher);
        String url = params.get("url");
        String s = params.get("s");
        String sp = params.getOrDefault("sp", "sig");

        if (url == null || s == null) return urlOrCipher;

        try {
            url = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
            s = URLDecoder.decode(s, StandardCharsets.UTF_8.name());

            DecipherFunction func = getDecipherFunction(html5playerUrl);
            if (func != null) {
                String sig = func.apply(s);
                url = setQueryParam(url, sp, sig);
            }
        } catch (Exception e) {
            log.error("Failed to decipher signature", e);
        }

        return transformN(url, html5playerUrl);
    }

    private String transformN(String url, String html5playerUrl) {
        try {
            Map<String, String> params = parseQueryString(url);
            String n = params.get("n");
            if (n == null) return url;

            return url;
        } catch (Exception e) {
            return url;
        }
    }

    private DecipherFunction getDecipherFunction(String playerUrl) {
        if (decipherCache.containsKey(playerUrl)) return decipherCache.get(playerUrl);

        try {
            String fullUrl = playerUrl.startsWith("http") ? playerUrl : "https://www.youtube.com" + playerUrl;
            Request request = new Request.Builder().url(fullUrl).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String script = response.body().string();
                    DecipherFunction func = extractDecipherFunction(script);
                    if (func != null) {
                        decipherCache.put(playerUrl, func);
                        return func;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch/extract decipher function", e);
        }
        return null;
    }

    private DecipherFunction extractDecipherFunction(String script) {
        Matcher matcher = Pattern.compile("\\.set\\(\"signature\",\\s*([a-zA-Z0-9$]+)\\(").matcher(script);
        if (!matcher.find()) {
            matcher = Pattern.compile("([a-zA-Z0-9$]+)\\s*=\\s*function\\([a-zA-Z0-9$]+\\)\\{\\s*[a-zA-Z0-9$]+\\s*=\\s*[a-zA-Z0-9$]+\\.split\\(\"\"\\)").matcher(script);
        }

        if (!matcher.find()) return null;
        String funcName = matcher.group(1);

        String funcPattern = "var\\s+" + Pattern.quote(funcName) + "\\s*=\\s*function\\([a-zA-Z0-9$]+\\)\\{(.+?)};";
        matcher = Pattern.compile(funcPattern, Pattern.DOTALL).matcher(script);
        if (!matcher.find()) {
            funcPattern = "function\\s+" + Pattern.quote(funcName) + "\\s*\\([a-zA-Z0-9$]+\\)\\{(.+?)}";
            matcher = Pattern.compile(funcPattern, Pattern.DOTALL).matcher(script);
        }

        if (!matcher.find()) return null;
        String body = matcher.group(1);

        matcher = Pattern.compile("([a-zA-Z0-9$]+)\\.([a-zA-Z0-9$]+)\\(").matcher(body);
        if (!matcher.find()) return null;
        String objName = matcher.group(1);

        String objPattern = "var\\s+" + Pattern.quote(objName) + "\\s*=\\s*\\{(.+?)};";
        matcher = Pattern.compile(objPattern, Pattern.DOTALL).matcher(script);
        if (!matcher.find()) return null;
        String objBody = matcher.group(1);

        return new DecipherFunction(body, objBody);
    }

    private Map<String, String> parseQueryString(String str) {
        Map<String, String> params = new HashMap<>();
        String query = str;
        if (str.contains("?")) query = str.split("\\?")[1];

        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length > 1) {
                params.put(pair[0], pair[1]);
            } else if (pair.length == 1) {
                params.put(pair[0], "");
            }
        }
        return params;
    }

    private String setQueryParam(String url, String key, String value) throws UnsupportedEncodingException {
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        if (url.contains(key + "=")) {
            return url.replaceAll(key + "=[^&]*", key + "=" + encodedValue);
        } else {
            return url + (url.contains("?") ? "&" : "?") + key + "=" + encodedValue;
        }
    }

    private static class DecipherFunction {
        private final String body;
        private final Map<String, String> operations = new HashMap<>();

        public DecipherFunction(String body, String objBody) {
            this.body = body;
            String[] parts = objBody.split("\\n");
            for (String part : parts) {
                Matcher m = Pattern.compile("([a-zA-Z0-9$]+):\\s*function\\(a,b\\)\\{(.+?)}").matcher(part);
                if (m.find()) {
                    String opName = m.group(1);
                    String opBody = m.group(2);
                    if (opBody.contains("reverse")) operations.put(opName, "reverse");
                    else if (opBody.contains("splice")) operations.put(opName, "splice");
                    else operations.put(opName, "swap");
                }
            }
        }

        public String apply(String s) {
            StringBuilder sb = new StringBuilder(s);
            String[] lines = body.split(";");
            for (String line : lines) {
                Matcher m = Pattern.compile("\\.([a-zA-Z0-9$]+)\\(a,(\\d+)\\)").matcher(line);
                if (m.find()) {
                    String opName = m.group(1);
                    int arg = Integer.parseInt(m.group(2));
                    String op = operations.get(opName);
                    if ("reverse".equals(op)) reverse(sb);
                    else if ("splice".equals(op)) splice(sb, arg);
                    else if ("swap".equals(op)) swap(sb, arg);
                }
            }
            return sb.toString();
        }

        private void reverse(StringBuilder sb) {
            sb.reverse();
        }

        private void splice(StringBuilder sb, int n) {
            sb.delete(0, n);
        }

        private void swap(StringBuilder sb, int n) {
            char temp = sb.charAt(0);
            sb.setCharAt(0, sb.charAt(n % sb.length()));
            sb.setCharAt(n % sb.length(), temp);
        }
    }
}
