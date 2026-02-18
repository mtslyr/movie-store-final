package ru.practicum.moviehub.exception;

public class IllegalTitleAndYearException extends Exception {
    public IllegalTitleAndYearException() {
    }

    public IllegalTitleAndYearException(String message) {
        super(message);
    }

    public IllegalTitleAndYearException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalTitleAndYearException(Throwable cause) {
        super(cause);
    }

    public IllegalTitleAndYearException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
