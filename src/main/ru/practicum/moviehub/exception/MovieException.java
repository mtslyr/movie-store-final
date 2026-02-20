package ru.practicum.moviehub.exception;

import java.util.List;

public class MovieException extends Exception {
    private final String message;
    private final List<String> details;

    private final int statusCode;
    public MovieException(String message, List<String> details, int statusCode) {
        this.details = details;
        this.message = message;
        this.statusCode = statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public List<String> getDetails() {
        return details;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
