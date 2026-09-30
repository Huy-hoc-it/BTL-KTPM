package com.example.cinema.modules.identity.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.IOException;

public record RegisterRequest(
        @JsonDeserialize(using = StrictStringDeserializer.class) String username,
        @JsonDeserialize(using = StrictStringDeserializer.class) String password
) {
    @Override
    public String toString() {
        return "RegisterRequest[redacted]";
    }

    public static class StrictStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return (String) context.handleUnexpectedToken(String.class, parser);
            }
            return parser.getText();
        }
    }
}
