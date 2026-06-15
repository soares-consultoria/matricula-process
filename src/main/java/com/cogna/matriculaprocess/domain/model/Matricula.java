package com.cogna.matriculaprocess.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

public record Matricula(
        String id,
        String alunoId,
        String businessKey,
        StatusMatricula status,
        Turma turma,
        Long cicloId,
        LocalDateTime dataMatricula) {

    /**
     * Compara os dias da semana da matrícula com os novos dias da turma,
     * ignorando ordem e duplicidades.
     */
    public boolean mesmosDias(List<String> novosDias) {
        List<String> diasAtuais = turma != null && turma.diasDaSemana() != null
                ? turma.diasDaSemana()
                : List.of();
        return new HashSet<>(diasAtuais).equals(new HashSet<>(novosDias != null ? novosDias : List.of()));
    }
}
