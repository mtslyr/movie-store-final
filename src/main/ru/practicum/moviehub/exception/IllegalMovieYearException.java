package ru.practicum.moviehub.exception;

public class IllegalMovieYearException extends Exception {
    public IllegalMovieYearException() {
    }

    public IllegalMovieYearException(String message) {
        super(message);
    }

    public IllegalMovieYearException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalMovieYearException(Throwable cause) {
        super(cause);
    }

    public IllegalMovieYearException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
