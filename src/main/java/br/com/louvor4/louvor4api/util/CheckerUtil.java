package br.com.louvor4.louvor4api.util;

import java.util.Collection;

public class CheckerUtil {
    public CheckerUtil() {
    }

    public static boolean isEmpty(Object valor) {
        return valor instanceof Collection ? ((Collection)valor).isEmpty() : "".equals(valor.toString().trim());
    }

    public static boolean notNullNorEmpty(Object valor) {
        return valor != null && !isEmpty(valor);
    }



}
