package com.cogna.matriculaprocess.adapter.in.kafka;

/**
 * Exceção que sinaliza uma mensagem malformada ou sem campos obrigatórios.
 *
 * <p>É marcada como não recuperável no error handler do consumidor: não há
 * benefício em refazer tentativas (retry) para um payload inválido, portanto a
 * mensagem é encaminhada diretamente para a <em>Dead Letter Topic</em> (DLT).</p>
 *
 * @author Equipe matricula-process
 * @see com.cogna.matriculaprocess.config.KafkaConsumerConfig
 */
public class MensagemInvalidaException extends RuntimeException {

    /**
     * Cria a exceção com uma mensagem descritiva.
     *
     * @param message descrição do problema encontrado
     */
    public MensagemInvalidaException(String message) {
        super(message);
    }

    /**
     * Cria a exceção com uma mensagem descritiva e a causa original.
     *
     * @param message descrição do problema encontrado
     * @param cause   causa raiz (ex.: erro de desserialização Jackson)
     */
    public MensagemInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
