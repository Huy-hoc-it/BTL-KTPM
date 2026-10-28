package com.example.cinema.modules.movie.api;

import com.example.cinema.config.JwtTokenService;
import com.example.cinema.modules.identity.business.*;
import com.example.cinema.modules.movie.business.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
    "cinema.theater.enabled=false", "cinema.booking.enabled=false",
    "management.endpoint.health.group.readiness.include=readinessState"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MovieApiTest {
    static final String BODY = """
        {"title":"Interstellar","description":"Space travel","durationMinutes":169,
         "ageRating":"T13","releaseDate":"2026-10-01","posterUrl":"https://example.com/poster.jpg","status":"NOW_SHOWING"}
        """;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtTokenService tokens;
    @MockitoBean(name="jpaMovieRepository") MovieRepository repository;
    @MockitoBean(name="userRepositoryAdapter") UserRepository users;
    final Map<UUID,Movie> rows = new ConcurrentHashMap<>();
    String admin;

    @BeforeEach
    void setUp() {
        rows.clear();
        admin = "Bearer " + tokens.issue(new AuthenticatedUser(UUID.randomUUID(),User.Role.ADMIN));
        when(repository.save(any(Movie.class))).thenAnswer(call -> { Movie movie=call.getArgument(0); rows.put(movie.getId(),movie); return movie; });
        when(repository.findUndeletedById(any(UUID.class))).thenAnswer(call -> visible(call.getArgument(0)));
        when(repository.findUndeletedByIdForUpdate(any(UUID.class))).thenAnswer(call -> visible(call.getArgument(0)));
        when(repository.findUndeleted(anyInt(),anyInt(),nullable(MovieStatus.class))).thenAnswer(call -> {
            int page=call.getArgument(0), size=call.getArgument(1); MovieStatus status=call.getArgument(2);
            List<Movie> all=rows.values().stream().filter(m -> m.getDeletedAt()==null && (status==null || m.getStatus()==status))
                    .sorted(Comparator.comparing(Movie::getCreatedAt).thenComparing(Movie::getId).reversed()).toList();
            return new MoviePage(all.stream().skip((long)(page-1)*size).limit(size).toList(),all.size());
        });
    }
    Optional<Movie> visible(UUID id) { return Optional.ofNullable(rows.get(id)).filter(m -> m.getDeletedAt()==null); }
    String createMovie() throws Exception {
        String body=mvc.perform(post("/api/v1/admin/movies").header("Authorization",admin)
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isCreated())
                .andExpect(header().exists("Location")).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data").path("id").asText();
    }

    @Test
    void adminCrudAndPublicReadsUseTheDocumentedEnvelope() throws Exception {
        String id=createMovie();
        mvc.perform(get("/api/v1/movies/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Interstellar"));
        mvc.perform(get("/api/v1/movies?status=NOW_SHOWING&pageSize=1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.totalItems").value(1)).andExpect(jsonPath("$.meta.totalPages").value(1));
        mvc.perform(patch("/api/v1/admin/movies/"+id).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated\",\"posterUrl\":null}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated")).andExpect(jsonPath("$.data.durationMinutes").value(169))
                .andExpect(jsonPath("$.data.posterUrl").isEmpty());
        mvc.perform(delete("/api/v1/admin/movies/"+id).header("Authorization",admin))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(rows.get(UUID.fromString(id)).getDeletedAt()).isNotNull();
        mvc.perform(get("/api/v1/movies/"+id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("MOVIE_NOT_FOUND"));
        mvc.perform(get("/api/v1/movies")).andExpect(status().isOk()).andExpect(jsonPath("$.meta.totalPages").value(0));
        mvc.perform(delete("/api/v1/admin/movies/"+id).header("Authorization",admin)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource("malformedBodies")
    void invalidJsonTypesAre400(String body) throws Exception {
        mvc.perform(post("/api/v1/admin/movies").header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("MALFORMED_REQUEST"));
        assertThat(rows).isEmpty();
    }
    static Stream<String> malformedBodies() {
        return Stream.of("{",BODY.replace("169","169.5"),BODY.replace("169","\"169\""),BODY.replace("169","true"),
                BODY.replace("\"Interstellar\"","123"),BODY.replace("\"T13\"","0"),BODY.replace("\"NOW_SHOWING\"","1"),
                BODY.replace("NOW_SHOWING","UPCOMING"),BODY.replace("2026-10-01","2026-02-30"),
                BODY.replace("2026-10-01","2026-10-01T10:00:00"),BODY.replace("\"2026-10-01\"","[2026,10,1]"),
                BODY.replace("2026-10-01",""),
                BODY.replace("\"title\":","\"extra\":true,\"title\":"));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void invalidValuesAre422WithFieldDetailsAndRequestId(String body) throws Exception {
        String requestId=UUID.randomUUID().toString();
        mvc.perform(post("/api/v1/admin/movies").header("Authorization",admin).header("X-Request-Id",requestId)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnprocessableEntity())
                .andExpect(header().string("X-Request-Id",requestId)).andExpect(jsonPath("$.error.requestId").value(requestId))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR")).andExpect(jsonPath("$.error.details[0].field").exists())
                .andExpect(jsonPath("$.error.details[0].reason").exists());
        assertThat(rows).isEmpty();
    }
    static Stream<String> invalidBodies() {
        return Stream.of("{}",BODY.replace("Interstellar"," "),BODY.replace("Interstellar","x".repeat(201)),
                BODY.replace("Space travel",""),BODY.replace("169","0"),BODY.replace("169","-1"),
                BODY.replace("\"T13\"","null"),BODY.replace("\"2026-10-01\"","null"),BODY.replace("\"NOW_SHOWING\"","null"),
                BODY.replace("https://example.com/poster.jpg","x".repeat(501)));
    }

    @ParameterizedTest
    @ValueSource(strings={"title","description","durationMinutes","ageRating","releaseDate","status"})
    void patchRejectsExplicitNullForRequiredFields(String field) throws Exception {
        String id=createMovie();
        mvc.perform(patch("/api/v1/admin/movies/"+id).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\""+field+"\":null}")).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.details[0].field").value(field));
        assertThat(rows.get(UUID.fromString(id)).getTitle()).isEqualTo("Interstellar");
    }

    @ParameterizedTest
    @ValueSource(strings={"{}","{\"title\":\" \"}","{\"durationMinutes\":0}"})
    void invalidPatchDoesNotChangeMovie(String body) throws Exception {
        String id=createMovie();
        mvc.perform(patch("/api/v1/admin/movies/"+id).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity());
        assertThat(rows.get(UUID.fromString(id)).getTitle()).isEqualTo("Interstellar");
    }

    @ParameterizedTest
    @ValueSource(strings={"page=0","page=-1","pageSize=0","pageSize=101"})
    void invalidPageIs422(String query) throws Exception {
        mvc.perform(get("/api/v1/movies?"+query).header("Authorization",admin)).andExpect(status().isUnprocessableEntity());
    }

    @ParameterizedTest
    @ValueSource(strings={"/api/v1/movies/not-a-uuid","/api/v1/movies?page=x","/api/v1/movies?status=UPCOMING"})
    void malformedRouteAndQueryAre400(String path) throws Exception {
        mvc.perform(get(path).header("Authorization",admin)).andExpect(status().isBadRequest());
    }

    @Test
    void enforcesAdminWithRealSignedTokens() throws Exception {
        mvc.perform(post("/api/v1/admin/movies").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnauthorized());
        String customer=tokens.issue(new AuthenticatedUser(UUID.randomUUID(),User.Role.CUSTOMER));
        mvc.perform(post("/api/v1/admin/movies").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/movies").header("Authorization","Bearer broken").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        assertThat(rows).isEmpty();
    }

    @Test
    void missingMovieUses404ForReadPatchAndDelete() throws Exception {
        String id=UUID.randomUUID().toString();
        mvc.perform(get("/api/v1/movies/"+id).header("Authorization",admin)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/admin/movies/"+id).header("Authorization",admin).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"New\"}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("MOVIE_NOT_FOUND"));
        mvc.perform(delete("/api/v1/admin/movies/"+id).header("Authorization",admin)).andExpect(status().isNotFound());
    }

    @Test
    void openApiDescribesFiveMovieOperationsAndSecurity() throws Exception {
        JsonNode docs=mapper.readTree(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode paths=docs.path("paths");
        assertThat(paths.path("/api/v1/movies").has("get")).isTrue();
        assertThat(paths.path("/api/v1/movies").path("get").has("security")).isFalse();
        assertThat(paths.path("/api/v1/movies/{movieId}").path("get").has("security")).isFalse();
        assertThat(paths.path("/api/v1/admin/movies").path("post").path("security").get(0).has("bearerAuth")).isTrue();
        assertThat(paths.path("/api/v1/admin/movies/{movieId}").path("patch").path("responses").has("422")).isTrue();
        assertThat(paths.path("/api/v1/admin/movies/{movieId}").path("delete").path("responses").has("204")).isTrue();
        assertThat(docs.path("components").path("schemas").path("UpdateMovieRequest").path("properties").has("providedFields")).isFalse();
    }
}
