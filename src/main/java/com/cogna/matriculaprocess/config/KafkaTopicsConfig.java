package com.cogna.matriculaprocess.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * A aplicação é responsável por criar os tópicos na inicialização
 * (via KafkaAdmin auto-configurado pelo Spring Boot).
 */
@Configuration
public class KafkaTopicsConfig {

    @Value("${app.kafka.topics.turma-atualizada}")
    private String turmaAtualizada;

    @Value("${app.kafka.topics.matricula-atualizada}")
    private String matriculaAtualizada;

    @Value("${app.kafka.topics.partitions:3}")
    private int partitions;

    @Bean
    public NewTopic turmaAtualizadaTopic() {
        return TopicBuilder.name(turmaAtualizada).partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic turmaAtualizadaDltTopic() {
        // Mesmo número de partições do tópico de origem: o
        // DeadLetterPublishingRecoverer preserva a partição original.
        return TopicBuilder.name(turmaAtualizada + ".DLT").partitions(partitions).replicas(1).build();
    }

    @Bean
    public NewTopic matriculaAtualizadaTopic() {
        return TopicBuilder.name(matriculaAtualizada).partitions(partitions).replicas(1).build();
    }
}
