package com.cogna.matriculaprocess.application.dto;

import java.time.LocalDateTime;
import java.util.List;

public record MatriculaAtualizadaEvento(
        String matriculaId,
        String alunoId,
        String businessKey,
        Long cicloId,
        List<String> diasDaSemanaAnterior,
        List<String> diasDaSemanaNovo,
        LocalDateTime dataAtualizacao) {
}
