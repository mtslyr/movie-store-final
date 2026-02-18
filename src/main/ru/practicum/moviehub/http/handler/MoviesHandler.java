package ru.practicum.moviehub.http.handler;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.exception.EmptyMovieTitleException;
import ru.practicum.moviehub.exception.IllegalMovieYearException;
import ru.practicum.moviehub.exception.IllegalTitleAndYearException;
import ru.practicum.moviehub.exception.TooLongMovieTitleException;
import ru.practicum.moviehub.http.BaseHttpHandler;
import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieRequest;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesServer server;

    public MoviesHandler(MoviesServer server) {
        this.server = server;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] pathParts = exchange.getRequestURI().getPath().split("/");

        System.out.println("Обрабатываю запрос %s %s".formatted(method, exchange.getRequestURI()));

        if (method.equalsIgnoreCase("GET")) {
            if (pathParts.length == 2) { // /movies
                handleGetMoviesList(exchange);
            } else if (pathParts.length == 3) { // /movies/{id}
                handleGetMovie(exchange, pathParts[2]);
            }
        } else if (method.equalsIgnoreCase("POST")) {
            handlePostMovie(exchange);
        } else if (method.equalsIgnoreCase("DELETE")) {
            handleDeleteMovie(exchange);
        }
    }

    private void handleDeleteMovie(HttpExchange exchange) throws IOException {
        String id = exchange.getRequestURI().getPath().split("/")[2];

        try {
            server.deleteMovie(id);
            sendNoContent(exchange, 204);
        } catch (NoSuchElementException e) {
            ErrorResponse noSuchMovieResponse = new ErrorResponse("Фильм не найден");
            noSuchMovieResponse.setDetails(List.of(NO_SUCH_MOVIE_RESPONSE));
            String json = GSON.toJson(noSuchMovieResponse);
            sendJson(exchange, 404, json);
        } catch (NumberFormatException e) {
            ErrorResponse invalidIdFormatResponse = new ErrorResponse("Ошибка валидации");
            invalidIdFormatResponse.setDetails(List.of(INVALID_ID_FORMAT));
            String json = GSON.toJson(invalidIdFormatResponse);
            sendJson(exchange, 422, json);
        }
    }

    private void handlePostMovie(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestHeaders().getFirst("Content-Type").equals(CT_JSON)) {
            sendNoContent(exchange, 415);
            return;
        }

        String body = new String(exchange.getRequestBody().readAllBytes());
        MovieRequest movieRequest = GSON.fromJson(body, MovieRequest.class);

        try {
            Movie saved = server.saveMovie(movieRequest);
            sendJson(exchange, 201, GSON.toJson(saved));
        } catch (IllegalTitleAndYearException e) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации");
            errorResponse.setDetails(List.of(TITLE_SHOULD_NOT_BE_EMPTY, YEAR_SHOULD_BE_BETWEEN));
            sendJson(exchange, 422, GSON.toJson(errorResponse));
        } catch (EmptyMovieTitleException e) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации");
            errorResponse.setDetails(List.of(TITLE_SHOULD_NOT_BE_EMPTY));
            sendJson(exchange, 422, GSON.toJson(errorResponse));
        } catch (IllegalMovieYearException e) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации");
            errorResponse.setDetails(List.of(YEAR_SHOULD_BE_BETWEEN));
            sendJson(exchange, 422, GSON.toJson(errorResponse));
        } catch (TooLongMovieTitleException e) {
            ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации");
            errorResponse.setDetails(List.of(TOO_LONG_MOVIE_TITLE));
            sendJson(exchange, 422, GSON.toJson(errorResponse));
        }
    }

    private void handleGetMoviesList(HttpExchange exchange) throws IOException {
        if (exchange.getRequestURI().getQuery() != null) {
            handleGetMoviesWithParams(exchange);
            return;
        }

        List<Movie> movies = server.getAllMovies();
        String json = GSON.toJson(movies);
        System.out.println("Подготовил ответ:\n%s".formatted(json));
        sendJson(exchange, 200, json);
    }

    private void handleGetMoviesWithParams(HttpExchange exchange) throws IOException {
        Map<String, String> params = resolveParams(exchange);

        try {
            List<Movie> responseList = new ArrayList<>();
            for (String param : params.keySet()) {
                switch (param) {
                    case "year":
                        Integer year = Integer.parseInt(params.get("year"));
                        Predicate<Movie> yearPredicate = m -> Objects.equals(m.getYear(), year);
                        responseList.addAll(server.getMovieByCondition(yearPredicate));
                }
            }
            String json = GSON.toJson(responseList);
            System.out.println("Подготовил ответ:\n%s".formatted(json));
            sendJson(exchange, 200, json);
        } catch (NumberFormatException e) {
            ErrorResponse invalidIdFormatResponse = new ErrorResponse("Ошибка валидации");
            invalidIdFormatResponse.setDetails(List.of("Некорректный параметр запроса — 'year'"));
            String json = GSON.toJson(invalidIdFormatResponse);
            sendJson(exchange, 400, json);
        }
    }

    private void handleGetMovie(HttpExchange exchange, String id) throws IOException {
        try {
            Movie movie = server.getMovie(id);
            String json = GSON.toJson(movie);
            sendJson(exchange, 200, json);
        } catch (NoSuchElementException e) {
            ErrorResponse noSuchMovieResponse = new ErrorResponse("Ошбика валидации");
            noSuchMovieResponse.setDetails(List.of(NO_SUCH_MOVIE_RESPONSE));
            String json = GSON.toJson(noSuchMovieResponse);
            sendJson(exchange, 404, json);
        } catch (NumberFormatException e) {
            ErrorResponse invalidIdFormatResponse = new ErrorResponse("Ошибка валидации");
            invalidIdFormatResponse.setDetails(List.of(INVALID_ID_FORMAT));
            String json = GSON.toJson(invalidIdFormatResponse);
            sendJson(exchange, 400, json);
        }
    }

    private Map<String, String> resolveParams(HttpExchange exchange) {
        String[] paramStr = exchange.getRequestURI().getQuery().split("=");
        Map<String, String> params = new HashMap<>();
        for (int i = 0; i < paramStr.length - 1; i = i + 2) {
            params.put(paramStr[i], paramStr[i + 1]);
        }

        return params;
    }
}
