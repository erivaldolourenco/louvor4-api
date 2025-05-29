package br.com.louvor4.api.exceptions;

import org.springframework.http.ProblemDetail;

import java.net.URI;

public class ExceptionResponse extends ProblemDetail{

    private String details;

    public ExceptionResponse(String details) {
        this.details = details;
        this.setType(URI.create("https://datatracker.ietf.org/doc/html/rfc7231#section-6"));

    }

    public ExceptionResponse comStatus(int status) {
        this.setStatus(status);
        return this;
    }

    public ExceptionResponse comTitle(String title) {
        this.setTitle(title);
        return this;
    }

    public ExceptionResponse comDetail(String detail) {
        this.setDetail(detail);
        return this;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
