package ru.practicum.moviehub.exception;

public class TooLongMovieTitleException extends Exception {
    public TooLongMovieTitleException() {
    }

    public TooLongMovieTitleException(String message) {
        super(message);
    }

    public TooLongMovieTitleException(String message, Throwable cause) {
        super(message, cause);
    }

    public TooLongMovieTitleException(Throwable cause) {
        super(cause);
    }

    public TooLongMovieTitleException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
