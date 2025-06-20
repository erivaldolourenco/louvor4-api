package br.com.louvor4.api.exceptions;

public class StorageException extends RuntimeException {
    public StorageException(String message,Throwable cause) {
        super(message, cause);
    }
}
