package com.example.cinema.modules.movie.api;

import com.example.cinema.modules.movie.business.AgeRating;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;

public class MovieAgeRatingDeserializer extends JsonDeserializer<AgeRating> {
    @Override
    public AgeRating deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            try {
                return AgeRating.valueOf(parser.getText());
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw InvalidFormatException.from(parser, "Use a valid ageRating value", parser.getText(), AgeRating.class);
    }
}
