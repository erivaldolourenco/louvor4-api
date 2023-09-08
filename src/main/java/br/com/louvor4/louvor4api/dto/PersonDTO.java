package br.com.louvor4.louvor4api.dto;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class PersonDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String nome;
    private String sobrenome;
    private String email;
    private Date dataNascimento;
    private Date atualizacao;

    public PersonDTO() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(Date dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public Date getAtualizacao() {
        return atualizacao;
    }

    public void setAtualizacao(Date atualizacao) {
        this.atualizacao = atualizacao;
    }
}
