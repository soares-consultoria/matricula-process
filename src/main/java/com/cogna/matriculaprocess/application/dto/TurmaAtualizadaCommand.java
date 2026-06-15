package com.cogna.matriculaprocess.application.dto;

import java.util.List;

/**
 * Comando de aplicação que representa a intenção de processar uma turma
 * atualizada.
 *
 * <p>É a forma desacoplada do evento Kafka de entrada: o adaptador de entrada
 * converte o payload recebido neste comando antes de acionar o caso de uso,
 * evitando que o domínio dependa do formato de transporte.</p>
 *
 * @param businessKey   chave de negócio das matrículas afetadas
 * @param codigoTurma   código da turma
 * @param diasDaSemana  novos dias da semana da turma
 * @param horarioInicio horário de início das aulas
 * @param horarioFim    horário de término das aulas
 * @param vagas         quantidade de vagas da turma
 * @param cicloId       identificador do ciclo a validar
 * @author Equipe matricula-process
 */
public record TurmaAtualizadaCommand(
        String businessKey,
        String codigoTurma,
        List<String> diasDaSemana,
        String horarioInicio,
        String horarioFim,
        Integer vagas,
        Long cicloId) {
}
