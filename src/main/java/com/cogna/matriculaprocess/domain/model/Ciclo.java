package com.cogna.matriculaprocess.domain.model;

import java.time.LocalDate;

/**
 * Ciclo de captura retornado pelo serviço externo.
 */
public record Ciclo(
        Long id,
        boolean ativo,
        LocalDate dataInicioCaptura,
        LocalDate dataFimCaptura) {

    /**
     * Vigente quando ativo e a data de processamento está dentro da janela de
     * captura: dataInicioCaptura <= hoje < dataFimCaptura.
     */
    public boolean vigente(LocalDate dataProcessamento) {
        return ativo
                && !dataProcessamento.isBefore(dataInicioCaptura)
                && dataProcessamento.isBefore(dataFimCaptura);
    }
}
