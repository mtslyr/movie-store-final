package ru.practicum.moviehub.http.handler;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.http.BaseHttpHandler;
import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.model.Movie;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

public class MovieByIdHandler extends BaseHttpHandler {

    private final MoviesServer server;

    public MovieByIdHandler(MoviesServer server) {
        this.server = server;
    }
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        logRequest(exchange);
        String method = exchange.getRequestMethod();
        String[] pathParts = exchange.getRequestURI().getPath().split("/");

        if (pathParts.length == 3) {
            switch (method) {
                case "GET":
                    handleGetMovie(exchange, pathParts[2]);
                    break;
                case "DELETE":
                    handleDeleteMovie(exchange);
                    break;
                default:
                    sendNoContent(exchange, 405);
            }
        }
    }

    private void handleDeleteMovie(HttpExchange exchange) throws IOException {
        String id = exchange.getRequestURI().getPath().split("/")[2];

        try {
            if (!validateNumberFormat.test(id)) {
                sendValidationError(exchange, List.of(INVALID_ID_FORMAT), 422);
            }

            server.deleteMovie(id);
            sendNoContent(exchange, 204);
        } catch (NoSuchElementException e) {
            ErrorResponse noSuchMovieResponse = new ErrorResponse("Фильм с ID = %s найден".formatted(id));
            noSuchMovieResponse.setDetails(List.of(NO_SUCH_MOVIE_RESPONSE));
            String json = GSON.toJson(noSuchMovieResponse);
            sendJson(exchange, 404, json);
        }
    }

    private void handleGetMovie(HttpExchange exchange, String id) throws IOException {
        try {
            if (!validateNumberFormat.test(id)) {
                sendValidationError(exchange, List.of(INVALID_ID_FORMAT), 400);
                return;
            }

            Movie movie = server.getMovie(id);
            String json = GSON.toJson(movie);
            sendJson(exchange, 200, json);
        } catch (NoSuchElementException e) {
            ErrorResponse noSuchMovieResponse = new ErrorResponse("Фильм с ID = %s найден".formatted(id));
            noSuchMovieResponse.setDetails(List.of(NO_SUCH_MOVIE_RESPONSE));
            String json = GSON.toJson(noSuchMovieResponse);
            sendJson(exchange, 404, json);
        }
    }
}
