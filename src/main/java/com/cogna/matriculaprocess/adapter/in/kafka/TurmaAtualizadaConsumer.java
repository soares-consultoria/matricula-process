package com.cogna.matriculaprocess.adapter.in.kafka;

import com.cogna.matriculaprocess.adapter.in.kafka.dto.TurmaAtualizadaEvent;
import com.cogna.matriculaprocess.application.dto.TurmaAtualizadaCommand;
import com.cogna.matriculaprocess.application.port.in.ProcessarTurmaAtualizadaUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador de entrada (driving adapter) que consome eventos do tópico
 * {@code turma-atualizada}.
 *
 * <p>Desserializa o payload JSON, popula o {@link MDC} com dados de
 * rastreabilidade (correlationId, businessKey, tópico/partição/offset) e
 * delega o processamento ao caso de uso. Payloads malformados ou sem campos
 * obrigatórios resultam em {@link MensagemInvalidaException}, tratada pelo
 * error handler como não recuperável (enviada diretamente à DLT).</p>
 *
 * @author Equipe matricula-process
 * @see ProcessarTurmaAtualizadaUseCase
 * @see com.cogna.matriculaprocess.config.KafkaConsumerConfig
 */
@Component
public class TurmaAtualizadaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TurmaAtualizadaConsumer.class);

    private final ProcessarTurmaAtualizadaUseCase useCase;
    private final ObjectMapper objectMapper;

    /**
     * Cria o consumidor com o caso de uso e o desserializador JSON.
     *
     * @param useCase      caso de uso a ser acionado para cada evento válido
     * @param objectMapper mapeador Jackson usado na desserialização do payload
     */
    public TurmaAtualizadaConsumer(ProcessarTurmaAtualizadaUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

    /**
     * Processa um registro recebido do tópico {@code turma-atualizada}.
     *
     * @param record registro Kafka com o payload JSON do evento de turma
     * @throws MensagemInvalidaException se o JSON for inválido ou faltarem
     *                                   campos obrigatórios (envia à DLT)
     */
    @KafkaListener(topics = "${app.kafka.topics.turma-atualizada}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumir(ConsumerRecord<String, String> record) {
        MDC.put("correlationId", UUID.randomUUID().toString());
        MDC.put("topic", record.topic());
        MDC.put("partition", String.valueOf(record.partition()));
        MDC.put("offset", String.valueOf(record.offset()));
        try {
            TurmaAtualizadaEvent event = objectMapper.readValue(record.value(), TurmaAtualizadaEvent.class);
            MDC.put("businessKey", event.businessKey());
            MDC.put("cicloId", String.valueOf(event.cicloId()));

            log.info("Evento turma-atualizada recebido. businessKey={} turma={} cicloId={}",
                    event.businessKey(),
                    event.turma() != null ? event.turma().codigo() : null,
                    event.cicloId());

            useCase.processar(toCommand(event));
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            // Payload inválido não é recuperável: o error handler envia direto para a DLT.
            throw new MensagemInvalidaException("Payload inválido no tópico " + record.topic(), e);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Converte o evento Kafka no comando de aplicação, validando a presença dos
     * campos obrigatórios.
     *
     * @param event evento desserializado do tópico de entrada
     * @return comando de aplicação correspondente
     * @throws MensagemInvalidaException se {@code businessKey}, {@code turma} ou
     *                                   {@code cicloId} estiverem ausentes
     */
    private TurmaAtualizadaCommand toCommand(TurmaAtualizadaEvent event) {
        TurmaAtualizadaEvent.TurmaPayload turma = event.turma();
        if (event.businessKey() == null || event.cicloId() == null || turma == null) {
            throw new MensagemInvalidaException(
                    "Evento sem campos obrigatórios (businessKey, turma, cicloId): " + event);
        }
        return new TurmaAtualizadaCommand(
                event.businessKey(),
                turma.codigo(),
                turma.diasDaSemana(),
                turma.horarioInicio(),
                turma.horarioFim(),
                turma.vagas(),
                event.cicloId());
    }
}
