package br.com.louvor4.api.exceptions.handler;

import br.com.louvor4.api.exceptions.ExceptionResponse;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.StorageException;
import br.com.louvor4.api.exceptions.TokenExpiredException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import static br.com.louvor4.api.exceptions.handler.Constants.*;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ExceptionResponse errorResponse = ExceptionResponse.create()
                .withDetails(ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage())
                .withStatus(status.value())
                .withTitle(ARGUMENTO_INVALIDO);
        return ResponseEntity.status(status).body(errorResponse);
    }

    @ExceptionHandler(NotFoundException.class)
    public final ResponseEntity<ExceptionResponse> notFoundExceptions(Exception ex, WebRequest request) {
        ExceptionResponse errorResponse = ExceptionResponse.create()
                .withDetails(ex.getMessage())
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withTitle(RECURSO_NAO_ENCONTRADO);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public final ResponseEntity<ExceptionResponse> tokenExpiredExceptions(Exception ex, WebRequest request){
        ExceptionResponse exceptionResponse =  ExceptionResponse.create()
                .withDetails(ex.getMessage())
                .withStatus(HttpStatus.FORBIDDEN.value())
                .withTitle(TOKEN_EXPIRADO);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exceptionResponse);

    }

    @ExceptionHandler(BadCredentialsException.class)
    public final ResponseEntity<ExceptionResponse> badCredentialsException(Exception ex, WebRequest request) {
        ExceptionResponse errorResponse = ExceptionResponse.create()
                .withDetails(ex.getMessage())
                .withStatus(HttpStatus.UNAUTHORIZED.value())
                .withTitle(ERRO_DE_PERMISSAO);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public final ResponseEntity<ExceptionResponse> usernameNotFoundException(Exception ex, WebRequest request){
        ExceptionResponse exceptionResponse = ExceptionResponse.create()
                .withDetails(ex.getMessage())
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withTitle(RECURSO_NAO_ENCONTRADO);
        return  ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    @ExceptionHandler(StorageException.class)
    public final ResponseEntity<ExceptionResponse> storageException(Exception ex, WebRequest request){
        ExceptionResponse exceptionResponse = ExceptionResponse.create()
                .withDetails(ex.getMessage())
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withTitle(ERRO_AO_SALVAR_ARQUIVO);
        return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
    }

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ExceptionResponse> handleAllExceptions(Exception ex, WebRequest request){
        ExceptionResponse exceptionResponse = ExceptionResponse.create()
                .withDetails(ex.getMessage())
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withTitle(ERRO_INTERNO);
        return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
    }




}
