package com.example.cinema.modules.movie.api;

import com.example.cinema.modules.movie.business.MovieStatus;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;

public class MovieStatusDeserializer extends JsonDeserializer<MovieStatus> {
    @Override
    public MovieStatus deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            try {
                return MovieStatus.valueOf(parser.getText());
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw InvalidFormatException.from(parser, "Use a valid status value", parser.getText(), MovieStatus.class);
    }
}
