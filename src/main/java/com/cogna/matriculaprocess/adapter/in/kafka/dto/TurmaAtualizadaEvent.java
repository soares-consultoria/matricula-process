package com.cogna.matriculaprocess.adapter.in.kafka.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Representação do payload JSON recebido no tópico {@code turma-atualizada}.
 *
 * <p>Campos desconhecidos são ignorados para tolerar a evolução do contrato do
 * produtor sem quebrar a desserialização.</p>
 *
 * @param businessKey chave de negócio das matrículas afetadas
 * @param turma       dados da turma atualizada
 * @param cicloId     identificador do ciclo a validar
 * @author Equipe matricula-process
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TurmaAtualizadaEvent(
        String businessKey,
        TurmaPayload turma,
        Long cicloId) {

    /**
     * Bloco {@code turma} aninhado no evento de entrada.
     *
     * @param codigo        código da turma
     * @param diasDaSemana  novos dias da semana da turma
     * @param horarioInicio horário de início das aulas
     * @param horarioFim    horário de término das aulas
     * @param vagas         quantidade de vagas
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TurmaPayload(
            String codigo,
            List<String> diasDaSemana,
            String horarioInicio,
            String horarioFim,
            Integer vagas) {
    }
}
