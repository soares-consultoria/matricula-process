package com.cogna.matriculaprocess.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CicloTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 15);
    private static final LocalDate FIM = LocalDate.of(2026, 7, 1);

    @Test
    @DisplayName("vigente quando ativo e data dentro da janela de captura")
    void vigenteDentroDaJanela() {
        Ciclo ciclo = new Ciclo(20261L, true, INICIO, FIM);
        assertThat(ciclo.vigente(LocalDate.of(2026, 6, 12))).isTrue();
    }

    @Test
    @DisplayName("vigente no primeiro dia da janela (inclusivo)")
    void vigenteNoInicioInclusivo() {
        Ciclo ciclo = new Ciclo(20261L, true, INICIO, FIM);
        assertThat(ciclo.vigente(INICIO)).isTrue();
    }

    @Test
    @DisplayName("não vigente no último dia da janela (fim exclusivo)")
    void naoVigenteNoFimExclusivo() {
        Ciclo ciclo = new Ciclo(20261L, true, INICIO, FIM);
        assertThat(ciclo.vigente(FIM)).isFalse();
    }

    @Test
    @DisplayName("não vigente antes do início da captura")
    void naoVigenteAntesDoInicio() {
        Ciclo ciclo = new Ciclo(20261L, true, INICIO, FIM);
        assertThat(ciclo.vigente(INICIO.minusDays(1))).isFalse();
    }

    @Test
    @DisplayName("não vigente quando inativo, mesmo dentro da janela")
    void naoVigenteQuandoInativo() {
        Ciclo ciclo = new Ciclo(20262L, false, INICIO, FIM);
        assertThat(ciclo.vigente(LocalDate.of(2026, 6, 12))).isFalse();
    }

    @Test
    @DisplayName("não vigente quando a janela já expirou")
    void naoVigenteQuandoExpirado() {
        Ciclo ciclo = new Ciclo(20252L, true, LocalDate.of(2025, 1, 15), LocalDate.of(2025, 7, 1));
        assertThat(ciclo.vigente(LocalDate.of(2026, 6, 12))).isFalse();
    }
}
