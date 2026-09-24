package br.com.louvor4.entitlement.exceptions;

import br.com.louvor4.entitlement.services.UsernameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class EntitlementExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(EntitlementExceptionHandler.class);

    private final UsernameResolver usernameResolver;

    public EntitlementExceptionHandler(UsernameResolver usernameResolver) {
        this.usernameResolver = usernameResolver;
    }

    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<ProblemDetail> planLimitExceeded(PlanLimitExceededException ex, WebRequest request) {
        logger.warn("PlanLimitExceededException: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Limite do plano atingido");
        problem.setDetail(ex.getMessage());
        problem.setType(URI.create("https://datatracker.ietf.org/doc/html/rfc7231#section-6"));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(EntitlementMisconfiguredException.class)
    public ResponseEntity<ProblemDetail> entitlementMisconfigured(EntitlementMisconfiguredException ex, WebRequest request) {
        logger.error("EntitlementMisconfiguredException: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Erro ao verificar o plano");
        problem.setDetail(EntitlementMisconfiguredException.USER_MESSAGE);
        problem.setType(URI.create("https://datatracker.ietf.org/doc/html/rfc7231#section-6"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(NoActiveSubscriptionException.class)
    public ResponseEntity<ProblemDetail> noActiveSubscription(NoActiveSubscriptionException ex, WebRequest request) {
        logger.error(ex.getMessage());
        String username = usernameResolver.findUsername(ex.getUserId()).orElse(null);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Plano não encontrado");
        problem.setDetail(NoActiveSubscriptionException.buildUserMessage(username));
        problem.setType(URI.create("https://datatracker.ietf.org/doc/html/rfc7231#section-6"));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }
}
