package com.cogna.matriculaprocess.adapter.in.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TurmaAtualizadaEvent(
        String businessKey,
        TurmaPayload turma,
        Long cicloId) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TurmaPayload(
            String codigo,
            List<String> diasDaSemana,
            String horarioInicio,
            String horarioFim,
            Integer vagas) {
    }
}
