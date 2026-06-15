package com.cogna.matriculaprocess.domain.model;

import java.time.LocalDate;

/**
 * Ciclo de captura de matrículas retornado pelo serviço REST externo de ciclos.
 *
 * <p>Representa a janela de tempo em que um ciclo letivo aceita processamento de
 * matrículas. A regra de vigência ({@link #vigente(LocalDate)}) é o coração da
 * decisão de processar ou descartar um evento recebido.</p>
 *
 * @param id                identificador do ciclo (ex.: {@code 20261})
 * @param ativo             indica se o ciclo está ativo
 * @param dataInicioCaptura primeiro dia (inclusivo) da janela de captura
 * @param dataFimCaptura    dia de término (exclusivo) da janela de captura
 * @author Equipe matricula-process
 */
public record Ciclo(
        Long id,
        boolean ativo,
        LocalDate dataInicioCaptura,
        LocalDate dataFimCaptura) {

    /**
     * Determina se o ciclo está vigente para a data de processamento informada.
     *
     * <p>O ciclo é considerado vigente quando <strong>ambas</strong> as
     * condições são verdadeiras:</p>
     * <ul>
     *     <li>{@code ativo == true}; e</li>
     *     <li>a data de processamento está dentro da janela de captura, com
     *     início inclusivo e fim exclusivo:
     *     {@code dataInicioCaptura <= dataProcessamento < dataFimCaptura}.</li>
     * </ul>
     *
     * @param dataProcessamento data em que a mensagem está sendo processada
     * @return {@code true} se o ciclo estiver ativo e dentro da janela de
     *         captura; {@code false} caso contrário
     */
    public boolean vigente(LocalDate dataProcessamento) {
        return ativo
                && !dataProcessamento.isBefore(dataInicioCaptura)
                && dataProcessamento.isBefore(dataFimCaptura);
    }
}
