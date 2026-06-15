package com.cogna.matriculaprocess.config;

import com.cogna.matriculaprocess.adapter.in.kafka.MensagemInvalidaException;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

/**
 * Configuração de tratamento de falhas do consumidor Kafka.
 *
 * <p>Define a política de resiliência: até 3 tentativas com backoff exponencial
 * (1s, 2s, 4s, limitado a 10s) e, esgotadas as tentativas, publicação na
 * <em>Dead Letter Topic</em> {@code <tópico>.DLT}. {@link MensagemInvalidaException}
 * é classificada como não recuperável e vai direto para a DLT, sem retry.</p>
 *
 * @author Equipe matricula-process
 * @see com.cogna.matriculaprocess.config.KafkaTopicsConfig
 * @see com.cogna.matriculaprocess.adapter.in.kafka.TurmaAtualizadaConsumer
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    /**
     * Cria o error handler do consumidor com retry e roteamento para a DLT.
     *
     * <p>O destino da DLT é resolvido explicitamente como {@code <tópico>.DLT},
     * preservando a partição de origem, para casar com os tópicos provisionados
     * em {@link KafkaTopicsConfig}.</p>
     *
     * @param kafkaTemplate template usado para publicar as mensagens na DLT
     * @return o {@link DefaultErrorHandler} configurado
     */
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // Resolver explícito: publica em "<tópico>.DLT", casando com o tópico
        // provisionado em KafkaTopicsConfig, e preserva a partição de origem.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(MensagemInvalidaException.class);
        errorHandler.setRetryListeners((record, ex, attempt) ->
                log.warn("Falha ao processar registro do tópico {} (tentativa {}): {}",
                        record.topic(), attempt, ex.getMessage()));
        return errorHandler;
    }
}
