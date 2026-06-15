package com.cogna.matriculaprocess.domain.model;

import java.util.List;

/**
 * Dados da turma associados a uma {@link Matricula}.
 *
 * @param codigo        código identificador da turma (ex.: {@code T2026-001})
 * @param diasDaSemana  dias da semana de aula (ex.: {@code [SEGUNDA, QUARTA]})
 * @param horarioInicio horário de início das aulas (formato {@code HH:mm})
 * @param horarioFim    horário de término das aulas (formato {@code HH:mm})
 * @author Equipe matricula-process
 */
public record Turma(
        String codigo,
        List<String> diasDaSemana,
        String horarioInicio,
        String horarioFim) {
}
