package com.cogna.matriculaprocess.domain.model;

import java.util.List;

public record Turma(
        String codigo,
        List<String> diasDaSemana,
        String horarioInicio,
        String horarioFim) {
}
