package me.emsockz.roserp.packer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
        for (String key : obj1.keySet()) {
            JsonElement value1 = obj1.get(key);
            JsonElement value2 = obj2.get(key);

            if (value2 != null && value1.isJsonObject() && value2.isJsonObject()) {
                result.add(key, mergeObjects(value1.getAsJsonObject(), value2.getAsJsonObject()));
            } else {
                result.add(key, value2 != null ? value2 : value1);
            }
        }

        for (String key : obj2.keySet()) {
            if (!result.has(key)) {
                result.add(key, obj2.get(key));
            }
        }

        return result;
    }
}
