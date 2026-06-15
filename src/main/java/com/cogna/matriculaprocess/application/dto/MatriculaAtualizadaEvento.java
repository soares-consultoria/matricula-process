package com.cogna.matriculaprocess.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Evento de saída publicado no tópico {@code matricula-atualizada} para cada
 * matrícula efetivamente atualizada.
 *
 * @param matriculaId          identificador da matrícula atualizada
 * @param alunoId              identificador do aluno
 * @param businessKey          chave de negócio da matrícula
 * @param cicloId              identificador do ciclo
 * @param diasDaSemanaAnterior dias da semana antes da atualização
 * @param diasDaSemanaNovo     dias da semana após a atualização
 * @param dataAtualizacao      data/hora em que o processamento ocorreu
 * @author Equipe matricula-process
 */
public record MatriculaAtualizadaEvento(
        String matriculaId,
        String alunoId,
        String businessKey,
        Long cicloId,
        List<String> diasDaSemanaAnterior,
        List<String> diasDaSemanaNovo,
        LocalDateTime dataAtualizacao) {
}
