package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.exception.EmptyMovieTitleException;
import ru.practicum.moviehub.exception.IllegalMovieYearException;
import ru.practicum.moviehub.exception.IllegalTitleAndYearException;
import ru.practicum.moviehub.exception.TooLongMovieTitleException;
import ru.practicum.moviehub.http.handler.MovieByIdHandler;
import ru.practicum.moviehub.http.handler.MoviesHandler;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieRequest;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class MoviesServer {

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

    public Movie getMovie(String id) throws NoSuchElementException, NumberFormatException {
        return moviesStore.getMovieById(Integer.parseInt(id));
    }

    public List<Movie> getMovieByCondition(Predicate<Movie> predicate) {
        return moviesStore.getByCondition(predicate);
    }

    public Movie saveMovie(MovieRequest request) throws EmptyMovieTitleException, IllegalMovieYearException, IllegalTitleAndYearException, TooLongMovieTitleException {
        if (request.getTitle().isEmpty() && !validateMovieYear.test(request)) {
            throw new IllegalTitleAndYearException();
        }

        if (request.getTitle().isEmpty()) {
            throw new EmptyMovieTitleException();
        }

        if (!validateMovieYear.test(request)) {
            throw new IllegalMovieYearException();
        }

        if (request.getTitle().length() > 100) {
            throw new TooLongMovieTitleException();
        }


        return moviesStore.saveMovie(request.getTitle(), request.getYear());
    }

    public void clearStorage() {
        this.moviesStore.clear();
    }

    public void initTestMovies() {
        this.moviesStore.initTestData();
    }

    public void deleteMovie(String idStr) throws NumberFormatException, NoSuchElementException {
        int id = Integer.parseInt(idStr);
        moviesStore.deleteById(id);
    }
}