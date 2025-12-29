package br.com.louvor4.api.exceptions.handler;

import br.com.louvor4.api.exceptions.ExceptionResponse;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.StorageException;
import br.com.louvor4.api.exceptions.TokenExpiredException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
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
    public final ResponseEntity<ExceptionResponse> tokenExpiredExceptions(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.create()
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
    public final ResponseEntity<ExceptionResponse> usernameNotFoundException(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.create()
                .withDetails(ex.getMessage())
                .withStatus(HttpStatus.NOT_FOUND.value())
                .withTitle(RECURSO_NAO_ENCONTRADO);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponse);
    }

    @ExceptionHandler(StorageException.class)
    public final ResponseEntity<ExceptionResponse> storageException(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.create()
                .withDetails(ex.getMessage())
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withTitle(ERRO_AO_SALVAR_ARQUIVO);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public final ResponseEntity<ExceptionResponse> dataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {

        String msg = "Dados inválidos.";
        String raw = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();

        // Ajuste os nomes conforme suas constraints reais
        if (raw != null && raw.contains("uq_users_email")) {
            msg = "E-mail já cadastrado.";
        } else if (raw != null && raw.contains("users.username")) {
            msg = "Nome de usuário já cadastrado.";
        }

        ExceptionResponse response = ExceptionResponse.create()
                .withDetails(msg)
                .withStatus(HttpStatus.CONFLICT.value())
                .withTitle("Conflito");

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    //    @ExceptionHandler(Exception.class)
//    public final ResponseEntity<ExceptionResponse> handleAllExceptions(Exception ex, WebRequest request){
//        ExceptionResponse exceptionResponse = ExceptionResponse.create()
//                .withDetails(ex.getMessage())
//                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                .withTitle(ERRO_INTERNO);
//        return  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
//    }
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ExceptionResponse> handleAllExceptions(Exception ex, WebRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.create()
                .withDetails("Ocorreu um erro inesperado. Tente novamente.")
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .withTitle(ERRO_INTERNO);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponse);
    }


}
