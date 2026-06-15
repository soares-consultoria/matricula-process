package com.cogna.matriculaprocess.adapter.in.kafka;

/**
 * Mensagem malformada ou sem campos obrigatórios. Não é recuperável por
 * retry: vai direto para a DLT.
 */
public class MensagemInvalidaException extends RuntimeException {

    public MensagemInvalidaException(String message) {
        super(message);
    }

    public MensagemInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
