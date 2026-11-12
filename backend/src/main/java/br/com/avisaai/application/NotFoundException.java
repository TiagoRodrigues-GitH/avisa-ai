package br.com.avisaai.application;

/** Registro inexistente (vira HTTP 404 no adaptador web). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what, Object id) {
        super(what + " não encontrado: " + id);
    }
}
