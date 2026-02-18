package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

public class MoviesStore {
    private final Map<Integer, Movie> store;

    private int currentId;

    public MoviesStore() {
        this.store = new HashMap<>();
        this.currentId = 0;
    }

    public MoviesStore(String pathToFile) {
        this.store = new HashMap<>();
        this.currentId = 0;
        readMoviesFromFile(pathToFile);
    }

    public Movie getMovieById(Integer id) throws NoSuchElementException {
        Movie movie = store.get(id);

        if (movie == null) {
            throw new NoSuchElementException("Фильм не найден");
        }

        return movie;
    }

    public List<Movie> getMovies() {
        return new ArrayList<>(store.values());
    }

    public Movie saveMovie(String title, Integer year) {
        int newId = ++currentId;
        Movie newMovie = new Movie(title, year, newId);

        if (!store.values().contains(newMovie)) {
            store.put(newId, newMovie);
            System.out.println("Сохранен фильм: %s".formatted(newMovie));
            return newMovie;
        } else {
            return store.values().stream()
                    .filter(m -> m.getTitle().equalsIgnoreCase(title) && m.getYear().equals(year))
                    .findFirst()
                    .get();
        }
    }

    public List<Movie> getByCondition(Predicate<Movie> predicate) {
        return store.values().stream()
                .filter(predicate)
                .toList();
    }

    private void readMoviesFromFile(String path) {
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            while (reader.ready()) {
                String newLine = reader.readLine();
                String[] parts = newLine.split(";");
                saveMovie(parts[0], Integer.parseInt(parts[1]));
            }
        } catch (IOException e) {
            System.out.println("Не удалось прочитать фильмы из файла.");
        } catch (NumberFormatException e) {
            System.out.println("Неправильный формат года.");
        }
    }

    public void initTestData() {
        this.readMoviesFromFile("src/test/movies.txt");
    }

    public void clear() {
        store.clear();
    }

    public void deleteById(int id) throws NoSuchElementException {
        if (!store.containsKey(id)) {
            throw new NoSuchElementException();
        }

        Movie movieToDelete = getMovieById(id);

        store.remove(id);
        System.out.println("Удален фильм: %s".formatted(movieToDelete));
    }
}