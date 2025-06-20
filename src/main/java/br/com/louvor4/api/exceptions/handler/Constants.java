package br.com.louvor4.api.exceptions.handler;

public class Constants {

    private Constants() {
        throw new UnsupportedOperationException("Esta é uma classe utilitária e não pode ser instanciada.");
    }

    public static final String ARGUMENTO_INVALIDO = "Argumento Inválido";
    public static final String RECURSO_NAO_ENCONTRADO = "Recurso não Encontrado";
    public static final String TOKEN_EXPIRADO = "O token expirado";
    public static final String ERRO_INTERNO = "Erro Interno servidor";
    public static final String ERRO_DE_PERMISSAO = "Erro de permissão";

    public static final String ERRO_AO_SALVAR_ARQUIVO = "Erro ao salvar arquivo";


}
