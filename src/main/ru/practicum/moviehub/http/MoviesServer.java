package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.exception.MovieException;
import ru.practicum.moviehub.http.handler.MovieByIdHandler;
import ru.practicum.moviehub.http.handler.MoviesHandler;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieRequest;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MoviesServer {

    public static final String INVALID_ID_FORMAT = "Некорректный ID";
    protected static final String TITLE_SHOULD_NOT_BE_EMPTY = "название не должно быть пустым";
    protected static final String TOO_LONG_MOVIE_TITLE = "название не должно превышать 100 символов";
    protected static final String YEAR_SHOULD_BE_BETWEEN = "год должен быть между 1888 и %d"
            .formatted(LocalDate.now().getYear() + 1);

    private final MoviesStore moviesStore;
    private final Map<String, BaseHttpHandler> handlers = Map.of(
            "/movies", new MoviesHandler(this),
            "/movies/", new MovieByIdHandler(this)

    );
    private final HttpServer server;

    private final Predicate<MovieRequest> validateMovieYear = request -> {
        int curYear = LocalDate.now().getYear() + 1;
        int year1888 = 1888;

        return request.getYear() >= year1888 && request.getYear() <= curYear;
    };

    public MoviesServer(MoviesStore moviesStore, int port) {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
            this.moviesStore = moviesStore;
            initContext();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    private void initContext() {
        for (Map.Entry<String, BaseHttpHandler> handler : handlers.entrySet()) {
            server.createContext(handler.getKey(), handler.getValue());
        }
    }

    public List<Movie> getAllMovies() {
        return moviesStore.getMovies();
    }

    public Movie getMovie(String id) throws MovieException {
        return moviesStore.getMovieById(Integer.parseInt(id));
    }

    public List<Movie> getMovieByCondition(Predicate<Movie> predicate) {
        return moviesStore.getByCondition(predicate);
    }

    public Movie saveMovie(MovieRequest request) throws MovieException {
        List<String> exceptionDetails = new ArrayList<>();

        if (request.getTitle().isEmpty()) {
            exceptionDetails.add(TITLE_SHOULD_NOT_BE_EMPTY);
        }

        if (!validateMovieYear.test(request)) {
            exceptionDetails.add(YEAR_SHOULD_BE_BETWEEN);
        }

        if (request.getTitle().length() > 100) {
            exceptionDetails.add(TOO_LONG_MOVIE_TITLE);
        }

        if (!exceptionDetails.isEmpty()) {
            throw new MovieException("Ошибка валидации", exceptionDetails, 422);
        } else  {
            return moviesStore.saveMovie(request.getTitle(), request.getYear());
        }
    }

    public void clearStorage() {
        this.moviesStore.clear();
    }

    public void initTestMovies() {
        this.moviesStore.initTestData();
    }

    public void deleteMovie(String idStr) throws MovieException {
        int id = Integer.parseInt(idStr);
        moviesStore.deleteById(id);
    }
}