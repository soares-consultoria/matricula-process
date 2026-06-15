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
 * Tratamento de falhas do consumer: retry com backoff exponencial e, esgotadas
 * as tentativas, publicação na DLT ({@code <tópico>.DLT}).
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

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
