package br.com.louvor4.louvor4api.util;

import br.com.louvor4.louvor4api.exceptions.NotFoundException;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.http.ResponseEntity;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import static br.com.louvor4.louvor4api.util.CheckerUtil.notNullNorEmpty;
import static org.springframework.http.ResponseEntity.ok;

public interface ReturnUtil {

    static <P> ResponseEntity<List<P>> convert(Class<P> projectionClass, List<? extends Serializable> listaRetorno) {
        List<P> retorno = listaRetorno.stream().map(o -> new SpelAwareProxyProjectionFactory().createProjection(projectionClass, o))
                .collect(Collectors.toList());
        return ok(retorno);
    }
    static <P> ResponseEntity<List<P>> convertOrThrow(Class<P> projectionClass, List<? extends Serializable> listaRetorno) {
        lancarSemResultadoExceptionSeNulo(listaRetorno);
        return convert(projectionClass, listaRetorno);
    }

    static void lancarSemResultadoExceptionSeNulo(Object retorno) {
        lancarSemResultadoExceptionSeNulo(retorno, "Registro não encontrado.");
    }

    static void lancarSemResultadoExceptionSeNulo(Object retorno, String mensagem) {
        if (!notNullNorEmpty(retorno)) throw new NotFoundException(mensagem);
    }
}
