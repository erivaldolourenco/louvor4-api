package br.com.louvor4.api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class ValidationException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
