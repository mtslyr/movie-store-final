package ru.practicum.moviehub.http.handler;

import com.sun.net.httpserver.HttpExchange;
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
        logRequest(exchange);
        String method = exchange.getRequestMethod();
        switch (method) {
            case "GET":
                handleGetMoviesList(exchange);
                break;
            case "POST":
                handlePostMovie(exchange);
                break;
            default:
                sendNoContent(exchange, 405);
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
            sendValidationError(exchange, List.of(TITLE_SHOULD_NOT_BE_EMPTY, YEAR_SHOULD_BE_BETWEEN), 422);
        } catch (EmptyMovieTitleException e) {
            sendValidationError(exchange, List.of(TITLE_SHOULD_NOT_BE_EMPTY), 422);
        } catch (IllegalMovieYearException e) {
            sendValidationError(exchange, List.of(YEAR_SHOULD_BE_BETWEEN), 422);
        } catch (TooLongMovieTitleException e) {
            sendValidationError(exchange, List.of(TOO_LONG_MOVIE_TITLE), 422);
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

        List<Movie> responseList = new ArrayList<>();
        for (String param : params.keySet()) {
            switch (param) {
                case "year":
                    if (!validateNumberFormat.test(params.get("year"))) {
                        sendValidationError(exchange, List.of("Некорректный параметр запроса — 'year'"), 400);
                        return;
                    }

                    Integer year = Integer.parseInt(params.get("year"));
                    Predicate<Movie> yearPredicate = m -> Objects.equals(m.getYear(), year);
                    responseList.addAll(server.getMovieByCondition(yearPredicate));
                default:
                    System.out.println("Передан необрабатываемый параметр %s".formatted(param));
            }
        }

        String json = GSON.toJson(responseList);
        System.out.println("Подготовил ответ:\n%s".formatted(json));
        sendJson(exchange, 200, json);
    }

    private Map<String, String> resolveParams(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        String[] paramStr = query.split("&");
        Map<String, String> params = new HashMap<>();
        for (String q : paramStr) {
            String paramName = q.split("=")[0];
            String value = q.split("=")[1];
            params.put(paramName, value);
        }

        return params;
    }
}
