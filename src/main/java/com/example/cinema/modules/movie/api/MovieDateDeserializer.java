package com.example.cinema.modules.movie.api;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class MovieDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            String value=parser.getText();
            if (value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                try { return LocalDate.parse(value); } catch (DateTimeParseException ignored) { }
            }
        }
        throw InvalidFormatException.from(parser,"Use a valid date in yyyy-MM-dd format",parser.getText(),LocalDate.class);
    }
}
