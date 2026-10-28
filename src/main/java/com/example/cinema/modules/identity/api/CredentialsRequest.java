package com.example.cinema.modules.identity.api;

import com.example.cinema.modules.identity.business.AccountPolicy;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.IOException;

public record CredentialsRequest(
        @Schema(description = "3–50 characters: letters, digits, dot, underscore, or hyphen", example = "alice_01")
        @NotNull @Size(min = 3, max = 50) @Pattern(regexp = "[a-zA-Z0-9._-]+")
        @JsonDeserialize(using = StrictStringDeserializer.class) String username,
        @Schema(description = "Password containing 8–16 characters", example = "password1")
        @NotNull @Size(min = 8, max = 16)
        @JsonDeserialize(using = StrictStringDeserializer.class) String password
) {
    public CredentialsRequest {
        username = AccountPolicy.normalizeUsername(username);
    }

    @Override
    public String toString() {
        return "CredentialsRequest[redacted]";
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
