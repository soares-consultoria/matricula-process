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

@Component
public class TurmaAtualizadaConsumer {

    private static final Logger log = LoggerFactory.getLogger(TurmaAtualizadaConsumer.class);

    private final ProcessarTurmaAtualizadaUseCase useCase;
    private final ObjectMapper objectMapper;

    public TurmaAtualizadaConsumer(ProcessarTurmaAtualizadaUseCase useCase, ObjectMapper objectMapper) {
        this.useCase = useCase;
        this.objectMapper = objectMapper;
    }

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
