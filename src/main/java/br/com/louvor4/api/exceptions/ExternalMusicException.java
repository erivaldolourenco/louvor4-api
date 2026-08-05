package br.com.louvor4.api.exceptions;

public class ExternalMusicException extends RuntimeException {
    public ExternalMusicException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalMusicException(String message) {
        super(message);
    }
}
