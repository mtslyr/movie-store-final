package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    final String title;

    final Integer year;
    final Integer id;

    public Movie(String title, Integer year, Integer id) {
        this.title = title;
        this.year = year;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public Integer getId() {
        return id;
    }

    public Integer getYear() {
        return year;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return Objects.equals(title, movie.title) && Objects.equals(year, movie.year);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, year);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "title='" + title + '\'' +
                ", year=" + year +
                '}';
    }
}