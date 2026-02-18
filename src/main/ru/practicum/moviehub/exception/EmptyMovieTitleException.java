package ru.practicum.moviehub.exception;

public class EmptyMovieTitleException extends Exception {

    public EmptyMovieTitleException() {
    }

    public EmptyMovieTitleException(String message) {
        super(message);
    }

    public EmptyMovieTitleException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmptyMovieTitleException(Throwable cause) {
        super(cause);
    }

    public EmptyMovieTitleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
