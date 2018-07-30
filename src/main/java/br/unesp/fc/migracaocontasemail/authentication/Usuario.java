/*
 * MIT License
 *
 * Copyright (c) 2018 Demitrius Belai
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package br.unesp.fc.migracaocontasemail.authentication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer idUsuario;
    private String usuario;
    private String nome;
    private String email;
    private String emailUnesp;
    private String cpf;
    private String remoteAddress;
    private String tokenType;
    private String tokenValue;
    private String sessionId;
    private List<EmailMigracao> emails = new ArrayList<>();
    private boolean hasGoogleCredential = false;

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmailUnesp() {
        return emailUnesp;
    }

    public void setEmailUnesp(String emailUnesp) {
        this.emailUnesp = emailUnesp;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<EmailMigracao> getEmails() {
        return Collections.unmodifiableList(emails);
    }

    public void addEmail(EmailMigracao emailMigracao) {
        emails.add(emailMigracao);
    }

    public EmailMigracao getEmail(String email) {
        return emails.stream().filter(e -> e.email.equals(email))
                .findAny().orElse(null);
    }

    public List<EmailMigracao> getEmailsValidos() {
        return emails.stream().filter(e -> e.getErro() == null).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return usuario;
    }

    public void hasGoogleCredential(boolean value) {
        hasGoogleCredential = value;
    }

    public boolean hasGoogleCredential() {
        return hasGoogleCredential;
    }

    public static class EmailMigracao implements Serializable {
        private final String email;
        private final String dominio;
        private boolean senhaValidada = false;
        private boolean migrarEmails = false;
        private boolean migrarContatos = false;
        private String erro = null;

        public EmailMigracao(String email, String dominio) {
            this.email = email;
            this.dominio = dominio;
        }

        public String getEmail() {
            return email;
        }

        public String getDominio() {
            return dominio;
        }

        public boolean isSenhaValidada() {
            return senhaValidada;
        }

        public void setSenhaValidada(boolean senhaValidada) {
            this.senhaValidada = senhaValidada;
        }

        public boolean isMigrarEmails() {
            return migrarEmails;
        }

        public void setMigrarEmails(boolean migrarEmails) {
            this.migrarEmails = migrarEmails;
        }

        public boolean isMigrarContatos() {
            return migrarContatos;
        }

        public void setMigrarContatos(boolean migrarContatos) {
            this.migrarContatos = migrarContatos;
        }

        public String getErro() {
            return erro;
        }

        public void setErro(String erro) {
            this.erro = erro;
        }

    }

}
