package com.cogna.matriculaprocess.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Provisionamento dos tópicos Kafka na inicialização da aplicação.
 *
 * <p>Os {@link NewTopic} declarados aqui são criados automaticamente pelo
 * {@code KafkaAdmin} (auto-configurado pelo Spring Boot), tornando o serviço
 * responsável por criar suas próprias estruturas — não há dependência de
 * criação manual de tópicos no broker.</p>
 *
 * @author Equipe matricula-process
 * @see KafkaConsumerConfig
 */
@Configuration
public class KafkaTopicsConfig {

    @Value("${app.kafka.topics.turma-atualizada}")
    private String turmaAtualizada;

    @Value("${app.kafka.topics.matricula-atualizada}")
    private String matriculaAtualizada;

    @Value("${app.kafka.topics.partitions:3}")
    private int partitions;

    /**
     * Tópico de entrada {@code turma-atualizada}, consumido pelo serviço.
     *
     * @return definição do tópico de entrada
     */
    @Bean
    public NewTopic turmaAtualizadaTopic() {
        return TopicBuilder.name(turmaAtualizada).partitions(partitions).replicas(1).build();
    }

    /**
     * DLT do tópico de entrada ({@code turma-atualizada.DLT}).
     *
     * <p>Criada com o mesmo número de partições do tópico de origem, pois o
     * {@code DeadLetterPublishingRecoverer} preserva a partição original.</p>
     *
     * @return definição do tópico de dead letter
     */
    @Bean
    public NewTopic turmaAtualizadaDltTopic() {
        return TopicBuilder.name(turmaAtualizada + ".DLT").partitions(partitions).replicas(1).build();
    }

    /**
     * Tópico de saída {@code matricula-atualizada}, publicado pelo serviço.
     *
     * @return definição do tópico de saída
     */
    @Bean
    public NewTopic matriculaAtualizadaTopic() {
        return TopicBuilder.name(matriculaAtualizada).partitions(partitions).replicas(1).build();
    }
}
