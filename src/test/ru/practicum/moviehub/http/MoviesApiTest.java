package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieRequest;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    static final int TEST_PORT = 8888;

    static final String PATH_TO_MOVIES = "src/test/movies.txt";
    static String BASE = "http://localhost:" + TEST_PORT;

    static final HttpResponse.BodyHandler<String> BODY_HANDLER = HttpResponse.BodyHandlers
            .ofString(StandardCharsets.UTF_8);

    static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    static MoviesServer server;
    static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(PATH_TO_MOVIES), TEST_PORT);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    @DisplayName("Возвращает пустой массив, если фильмов нет")
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        server.clearStorage();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        System.out.println("[REQUEST] %s %s".formatted(request.method(), request.uri()));

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);

        assertEquals(
                response.statusCode(),
                200,
                "Код ответа должен быть равен 200. Фактически – %d".formatted(response.statusCode())
        );

        List<Movie> movies = GSON.fromJson(response.body(), new ListOfMoviesTypeToken().getType());

        assertTrue(
                movies.isEmpty(),
                "В ответе не вернулся пустой список: %s".formatted(Arrays.toString(movies.toArray()))
        );
    }

    @Test
    @DisplayName("Возвращает 10 фильмов из тестового файла")
    void shouldReturn10MoviesWithTestFile() throws IOException, InterruptedException {

        server.initTestMovies();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        System.out.println("[REQUEST] %s %s".formatted(request.method(), request.uri()));

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);

        assertStatusCode(response, 200);

        List<Movie> movies = GSON.fromJson(response.body(), new ListOfMoviesTypeToken().getType());

        assertTrue(
                movies.size() == 10,
                "Ожидалось 10 элементов в списке. Фактически – %d".formatted(movies.size())
        );
    }

    @Test
    @DisplayName("Возвращает фильм по ID")
    void shouldReturnMovieById() throws IOException, InterruptedException {
        server.initTestMovies();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        List<Movie> movies = GSON.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        Movie movie = movies.get(0);
        Integer id = movie.getId();

        request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies/" + id))
                .build();

        response = client.send(request, BODY_HANDLER);

        assertStatusCode(response, 200);

        Movie movieById = GSON.fromJson(response.body(), Movie.class);

        assertEquals(
                movie,
                movieById,
                "Вернулись разные фильмы:\n%s\n%s".formatted(movie, movieById)
        );

    }

    @Test
    @DisplayName("Возвращает 404 для несуществующего ID фильма")
    void shouldReturn404WithNonExistedMovieId() throws IOException, InterruptedException {
        server.initTestMovies();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        List<Movie> movies = GSON.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        List<Integer> ids = movies.stream().map(Movie::getId).toList();

        Random random = new Random();
        int randomId = random.nextInt();

        while (ids.contains(randomId)) {
            randomId = random.nextInt();
        }

        request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies/" + randomId))
                .build();

        response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);

        assertStatusCode(response, 404);

        ErrorResponse error = GSON.fromJson(response.body(), ErrorResponse.class);

        assertEquals(
                error.getError(),
                "Ошибка валидации"
        );
    }

    @Test
    @DisplayName("Возвращает 400 для некорректного формата ID")
    void shouldReturn400WithInvalidIdFormat() throws IOException, InterruptedException {
        server.initTestMovies();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies/" + "invalid"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);

        assertStatusCode(response, 400);

        ErrorResponse error = GSON.fromJson(response.body(), ErrorResponse.class);

        assertEquals(
                error.getError(),
                "Ошибка валидации"
        );
    }

    @Test
    @DisplayName("Фильтрует фильмы по году через параметр year")
    void shouldReturnMovieByYearParam() throws IOException, InterruptedException {
        server.initTestMovies();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies?year=2010"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);
        assertStatusCode(response, 200);

        List<Movie> movies = GSON.fromJson(response.body(), new ListOfMoviesTypeToken().getType());

        assertTrue(
                movies.stream().allMatch(m -> m.getYear() == 2010)
        );
    }

    @Test
    @DisplayName("Возвращает 400 для некорректного значения параметра year")
    void shouldReturn400WithInvalidQueryValueFormat() throws IOException, InterruptedException {
        server.initTestMovies();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies?year=invalid"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);
        assertStatusCode(response, 400);

        ErrorResponse errorResponse = GSON.fromJson(response.body(), ErrorResponse.class);
        assertTrue(errorResponse.getError().equals("Ошибка валидации"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1888, 2026, 2027})
    @DisplayName("Создаёт фильм с допустимым годом (1888–2027)")
    void shouldCreateNewMovieRecord(int year) throws IOException, InterruptedException {
        String title = "Title";
        String jsonBody = GSON.toJson(new MovieRequest(title, year));

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);
        assertStatusCode(response, 201);
        Movie movie = GSON.fromJson(response.body(), Movie.class);

        assertTrue(
                movie.getTitle().equals(title)
                        && movie.getYear().equals(year)
                        && Objects.nonNull(movie.getId())
        );
    }

    @Test
    @DisplayName("Возвращает 422 для пустого названия фильма")
    void shouldReturn422ForCreatingMovieWithEmptyTitle() throws IOException, InterruptedException {
        String title = "";
        int year = 1990;
        String jsonBody = GSON.toJson(new MovieRequest(title, year));

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);
        assertStatusCode(response, 422);
        ErrorResponse errorResponse = GSON.fromJson(response.body(), ErrorResponse.class);

        assertTrue(
                errorResponse.getError().equals("Ошибка валидации")
                && errorResponse.getDetails().contains("название не должно быть пустым")
        );
    }

    @Test
    @DisplayName("Возвращает 422, если название длиннее 100 символов")
    void shouldReturn422ForTitleLongerThan100Chars() throws IOException, InterruptedException {
        String longTitle = "A".repeat(101);
        int year = 1990;
        String jsonBody = GSON.toJson(new MovieRequest(longTitle, year));


        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);


        assertHeaderContentTypeJson(response);
        assertStatusCode(response, 422);

        ErrorResponse errorResponse = GSON.fromJson(response.body(), ErrorResponse.class);


        assertTrue(
                errorResponse.getError().equals("Ошибка валидации") &&
                        errorResponse.getDetails().contains("название не должно превышать 100 символов")
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {1887, 2028})
    @DisplayName("Возвращает 422 для года вне диапазона (1888–2027)")
    void shouldReturn422ForCreatingMovieWithWrongYear(int year) throws IOException, InterruptedException {
        String title = "Title";
        String jsonBody = GSON.toJson(new MovieRequest(title, year));

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);
        assertStatusCode(response, 422);

        ErrorResponse errorResponse = GSON.fromJson(response.body(), ErrorResponse.class);
        assertTrue(
                errorResponse.getError().equals("Ошибка валидации")
                && errorResponse.getDetails().contains("год должен быть между 1888 и 2027")
        );
    }

    @Test
    @DisplayName("Возвращает полные детали ошибки для нескольких нарушений валидации")
    void shouldReturnFullDetailErrorMessage() throws IOException, InterruptedException {
        String title = "";
        String jsonBody = GSON.toJson(new MovieRequest(title, 1880));

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);
        assertStatusCode(response, 422);

        ErrorResponse errorResponse = GSON.fromJson(response.body(), ErrorResponse.class);
        assertTrue(
                errorResponse.getError().equals("Ошибка валидации")
                        && errorResponse.getDetails().contains("название не должно быть пустым")
                        && errorResponse.getDetails().contains("год должен быть между 1888 и 2027")
        );
    }

    @Test
    @DisplayName("Возвращает 415 для некорректного Content-Type")
    void shouldReturn415ForWrongCTHeader() throws IOException, InterruptedException {
        String title = "Title";
        String jsonBody = GSON.toJson(new MovieRequest(title, 1991));

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "text/html")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertStatusCode(response, 415);
    }

    @Test
    @DisplayName("Удаляет фильм по ID")
    void shouldDeleteMovie() throws IOException, InterruptedException {
        String title = "Title";
        String jsonBody = GSON.toJson(new MovieRequest(title, 1999));

        HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json; charset=UTF-8")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertHeaderContentTypeJson(response);
        assertStatusCode(response, 201);
        int id = GSON.fromJson(response.body(), Movie.class).getId();

        request = HttpRequest.newBuilder()
                .DELETE()
                .header("Content-Type", "application/json; charset=UTF-8")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies/" + id))
                .build();

        response = client.send(request, BODY_HANDLER);

        assertStatusCode(response, 204);

        request = HttpRequest.newBuilder()
                .GET()
                .header("Content-Type", "application/json; charset=UTF-8")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies/" + id))
                .build();

        response = client.send(request, BODY_HANDLER);

        assertStatusCode(response, 404);
    }

    @Test
    @DisplayName("Возвращает 404 при удалении несуществующего фильма")
    void shouldReturn404ForDeleteNonExistedMovie() throws IOException, InterruptedException {
        server.initTestMovies();

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        List<Movie> movies = GSON.fromJson(response.body(), new ListOfMoviesTypeToken().getType());
        List<Integer> ids = movies.stream().map(Movie::getId).toList();

        Random random = new Random();
        int randomId = random.nextInt();

        while (ids.contains(randomId)) {
            randomId = random.nextInt();
        }

        request = HttpRequest.newBuilder()
                .DELETE()
                .header("Content-Type", "application/json; charset=UTF-8")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies/" + randomId))
                .build();

        response = client.send(request, BODY_HANDLER);

        assertStatusCode(response, 404);
    }

    @Test
    @DisplayName("Возвращает 422 для некорректного формата ID при удалении")
    void shouldReturn422ForDeleteMovieWithInvalidIdFormat() throws IOException, InterruptedException {
        server.initTestMovies();

        HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .header("Content-Type", "application/json; charset=UTF-8")
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(BASE + "/movies/invalid"))
                .build();

        HttpResponse<String> response = client.send(request, BODY_HANDLER);

        assertStatusCode(response, 422);
    }

    private void assertHeaderContentTypeJson(HttpResponse<String> response) {
        String expected = "application/json; charset=UTF-8";
        String actual = response.headers().firstValue("Content-type").orElse("");
        assertEquals(
                actual,
                expected,
                "Ожидаемое значение: '%s'. Фактическое значение: '%s'"
                        .formatted(expected, actual)
        );
    }

    private void assertStatusCode(HttpResponse<String> response, int statusCode) {
        assertEquals(
                response.statusCode(),
                statusCode,
                "Код ответа должен быть равен %d. Фактически – %d".formatted(statusCode, response.statusCode())
        );
    }
}