package io.github.luoshenshi.internal;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Optional;

/**
 * Internal utility for safely navigating deeply nested JSON structures
 * without throwing NullPointerExceptions or JSONExceptions.
 */
public class JsonPath {

    public static Optional<JSONObject> getObject(JSONObject root, String... path) {
        Object current = root;
        for (String segment : path) {
            if (current == null) return Optional.empty();

            if (current instanceof JSONObject obj) {
                if (segment.startsWith("[") && segment.endsWith("]")) {
                    String filter = segment.substring(1, segment.length() - 1);
                    if (filter.contains("=")) {
                        return Optional.empty();
                    }
                    return Optional.empty();
                }
                current = obj.opt(segment);
            } else if (current instanceof JSONArray arr) {
                if (segment.startsWith("[") && segment.endsWith("]")) {
                    String filter = segment.substring(1, segment.length() - 1);
                    if (filter.contains("=")) {
                        String[] parts = filter.split("=", 2);
                        String key = parts[0];
                        String value = parts[1];
                        Object found = null;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item != null && value.equals(item.optString(key))) {
                                found = item;
                                break;
                            }
                        }
                        current = found;
                    } else {
                        try {
                            int index = Integer.parseInt(filter);
                            current = arr.opt(index);
                        } catch (NumberFormatException e) {
                            return Optional.empty();
                        }
                    }
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        }
        return (current instanceof JSONObject) ? Optional.of((JSONObject) current) : Optional.empty();
    }

    public static Optional<JSONArray> getArray(JSONObject root, String... path) {
        if (path.length == 0) return Optional.empty();

        String[] parentPath = new String[path.length - 1];
        System.arraycopy(path, 0, parentPath, 0, path.length - 1);
        String lastSegment = path[path.length - 1];

        Object parent;
        if (parentPath.length == 0) {
            parent = root;
        } else {
            parent = getRaw(root, parentPath);
        }

        if (parent instanceof JSONObject obj) {
            return Optional.ofNullable(obj.optJSONArray(lastSegment));
        } else if (parent instanceof JSONArray arr && lastSegment.startsWith("[") && lastSegment.endsWith("]")) {
            // Handle nested array indexing if ever needed
            return Optional.empty();
        }

        return Optional.empty();
    }

    public static String getString(JSONObject root, String fallback, String... path) {
        Object current = getRaw(root, path);
        return current != null ? current.toString() : fallback;
    }

    private static Object getRaw(JSONObject root, String... path) {
        Object current = root;
        for (String segment : path) {
            if (current == null) return null;

            if (current instanceof JSONObject obj) {
                if (segment.startsWith("[") && segment.endsWith("]")) return null;
                current = obj.opt(segment);
            } else if (current instanceof JSONArray arr) {
                if (segment.startsWith("[") && segment.endsWith("]")) {
                    String filter = segment.substring(1, segment.length() - 1);
                    if (filter.contains("=")) {
                        String[] parts = filter.split("=", 2);
                        String key = parts[0];
                        String value = parts[1];
                        Object found = null;
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item != null && value.equals(item.optString(key))) {
                                found = item;
                                break;
                            }
                        }
                        current = found;
                    } else {
                        try {
                            int index = Integer.parseInt(filter);
                            current = arr.opt(index);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        return current;
    }
}
