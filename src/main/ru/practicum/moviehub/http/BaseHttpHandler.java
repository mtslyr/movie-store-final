package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.exception.MovieException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected Predicate<String> validateNumberFormat = s -> s.matches("-?\\d+");

    public void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(status, json.getBytes(StandardCharsets.UTF_8).length);

        System.out.println("[HANDLER] Возвращаю ответ:\n%s".formatted(json));

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
            exchange.close();
        }
    }

    public void sendNoContent(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }

    public void sendError(HttpExchange exchange, MovieException e) throws IOException {
        ErrorResponse response = new ErrorResponse(e.getMessage());
        response.setDetails(e.getDetails());
        String json = GSON.toJson(response);
        sendJson(exchange, e.getStatusCode(), json);
    }

    protected void logRequest(HttpExchange exchange) {
        System.out.println("Обрабатываю запрос %s %s".formatted(exchange.getRequestMethod(), exchange.getRequestURI()));
    }
}