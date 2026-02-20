package ru.practicum.moviehub.http.handler;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.exception.MovieException;
import ru.practicum.moviehub.http.BaseHttpHandler;
import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.model.Movie;

import java.io.IOException;
import java.util.List;

import static ru.practicum.moviehub.http.MoviesServer.INVALID_ID_FORMAT;

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
                sendError(exchange, new MovieException("Ошибка валидации", List.of(INVALID_ID_FORMAT), 422));
                return;
            }
            server.deleteMovie(id);
            sendNoContent(exchange, 204);
        } catch (MovieException e) {
            sendError(exchange, e);
        }
    }

    private void handleGetMovie(HttpExchange exchange, String id) throws IOException {
        try {
            if (!validateNumberFormat.test(id)) {
                sendError(exchange, new MovieException("Ошибка валидации", List.of(INVALID_ID_FORMAT), 400));
                return;
            }

            Movie movie = server.getMovie(id);
            String json = GSON.toJson(movie);
            sendJson(exchange, 200, json);
        } catch (MovieException e) {
            sendError(exchange, e);
        }
    }
}
