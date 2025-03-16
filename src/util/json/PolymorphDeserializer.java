//
// https://github.com/iSharipov/gson-adapters/blob/master/src/main/java/io/github/isharipov/gson/adapters/PolymorphDeserializer.java
// by iSharipov
//
package util.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Arrays;

public class PolymorphDeserializer<T> implements JsonDeserializer<T> {

    @Override
    public T deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
            Class<?> typeClass = Class.forName(type.getTypeName());
            JsonClassType jsonType = typeClass.getDeclaredAnnotation(JsonClassType.class);
            String property = json.getAsJsonObject().get(jsonType.property()).getAsString();
            JsonClassSubType[] subtypes = jsonType.subTypes();
            Type subType = Arrays.stream(subtypes).filter(subtype -> subtype.name().equals(property))
            		.findFirst().orElseThrow(IllegalArgumentException::new).jsonClass();
            return context.deserialize(json, subType);
        } catch (Exception e) {
            throw new JsonParseException("!!! Failed to deserialize Json: ", e);
        }
    }
}