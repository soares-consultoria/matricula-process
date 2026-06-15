package com.cogna.matriculaprocess.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatriculaTest {

    private Matricula matriculaComDias(List<String> dias) {
        return new Matricula("id-1", "ALU-001", "BK", StatusMatricula.ATIVA,
                new Turma("T2026-001", dias, "19:00", "22:30"),
                20261L, LocalDateTime.of(2026, 2, 10, 8, 0));
    }

    @Test
    @DisplayName("mesmos dias quando listas idênticas")
    void mesmosDiasIdenticos() {
        Matricula matricula = matriculaComDias(List.of("SEGUNDA", "QUARTA", "SEXTA"));
        assertThat(matricula.mesmosDias(List.of("SEGUNDA", "QUARTA", "SEXTA"))).isTrue();
    }

    @Test
    @DisplayName("mesmos dias independem da ordem")
    void mesmosDiasOrdemDiferente() {
        Matricula matricula = matriculaComDias(List.of("SEXTA", "SEGUNDA", "QUARTA"));
        assertThat(matricula.mesmosDias(List.of("SEGUNDA", "QUARTA", "SEXTA"))).isTrue();
    }

    @Test
    @DisplayName("dias diferentes quando há dia a mais")
    void diasDiferentes() {
        Matricula matricula = matriculaComDias(List.of("SEGUNDA", "QUARTA"));
        assertThat(matricula.mesmosDias(List.of("SEGUNDA", "QUARTA", "SEXTA"))).isFalse();
    }

    @Test
    @DisplayName("dias nulos na matrícula são tratados como lista vazia")
    void diasNulos() {
        Matricula matricula = matriculaComDias(null);
        assertThat(matricula.mesmosDias(List.of("SEGUNDA"))).isFalse();
        assertThat(matricula.mesmosDias(List.of())).isTrue();
    }
}
