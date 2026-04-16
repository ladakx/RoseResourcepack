package me.ladakx.roserp.packer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class JsonMerger {
    public static byte[] mergeJson(InputStream existingJsonStream, InputStream newJsonStream) {
        JsonParser parser = new JsonParser();
        JsonObject existingJson = parser.parse(new InputStreamReader(existingJsonStream, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject newJson = parser.parse(new InputStreamReader(newJsonStream, StandardCharsets.UTF_8)).getAsJsonObject();

        JsonObject mergedJson = mergeObjects(existingJson, newJson);

        return mergedJson.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject mergeObjects(JsonObject obj1, JsonObject obj2) {
        JsonObject result = new JsonObject();

        obj1.entrySet().forEach(entry -> {
            String key = entry.getKey();
            JsonElement value1 = entry.getValue();
            JsonElement value2 = obj2.get(key);

            if (value2 != null && value1.isJsonObject() && value2.isJsonObject()) {
                result.add(key, mergeObjects(value1.getAsJsonObject(), value2.getAsJsonObject()));
            } else {
                result.add(key, value2 != null ? value2 : value1);
            }
        });

        obj2.entrySet().forEach(entry -> {
            if (!result.has(entry.getKey())) {
                result.add(entry.getKey(), entry.getValue());
            }
        });

        return result;
    }
}
