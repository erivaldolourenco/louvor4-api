package br.com.louvor4.api.exceptions;

import br.com.louvor4.api.shared.dto.ApiResponse;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class ExceptionResponse extends ProblemDetail{

    public ExceptionResponse() {
        this.setType(URI.create("https://datatracker.ietf.org/doc/html/rfc7231#section-6"));
    }

    public static ExceptionResponse create() {
        return new ExceptionResponse();
    }


    public ExceptionResponse withStatus(int status) {
        this.setStatus(status);
        return this;
    }

    public ExceptionResponse withTitle(String title) {
        this.setTitle(title);
        return this;
    }

    public ExceptionResponse withDetails(String details) {
        this.setDetail(details);
        return this;
    }
}
