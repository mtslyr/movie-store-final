package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected static final String NO_SUCH_MOVIE_RESPONSE = "Фильм не найден";
    protected static final String INVALID_ID_FORMAT = "Некорректный ID";

    protected static final String TITLE_SHOULD_NOT_BE_EMPTY = "название не должно быть пустым";
    protected static final String TOO_LONG_MOVIE_TITLE = "название не должно превышать 100 символов";
    protected static final String YEAR_SHOULD_BE_BETWEEN = "год должен быть между 1888 и %d"
            .formatted(LocalDate.now().getYear() + 1);

    public void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", CT_JSON);
        exchange.sendResponseHeaders(status, json.getBytes(StandardCharsets.UTF_8).length);

        System.out.println("[HANDLER] Возвращаю ответ:\n%s".formatted(json));

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void sendNoContent(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
    }
}