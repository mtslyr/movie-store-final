package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private final String error;
    private List<String> details;

    public ErrorResponse(String error) {
        this.error = error;
    }

    public String getError() {
        return error;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
}