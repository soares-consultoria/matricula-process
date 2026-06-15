package com.cogna.matriculaprocess.application.dto;

import java.util.List;

public record TurmaAtualizadaCommand(
        String businessKey,
        String codigoTurma,
        List<String> diasDaSemana,
        String horarioInicio,
        String horarioFim,
        Integer vagas,
        Long cicloId) {
}
