package br.com.louvor4.entitlement.exceptions;

public class EntitlementMisconfiguredException extends RuntimeException {

    public static final String USER_MESSAGE =
            "Não foi possível verificar os limites do seu plano. Tente novamente mais tarde ou entre em contato com o suporte.";

    public EntitlementMisconfiguredException(String planName, String key, String value) {
        super(value == null
                ? "Entitlement '" + key + "' não configurado para o plano " + planName
                : "Entitlement '" + key + "' do plano " + planName + " tem valor numérico inválido: '" + value + "'");
    }
}
