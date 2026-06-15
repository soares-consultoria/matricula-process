package com.cogna.matriculaprocess.domain.model;

/**
 * Situação possível de uma {@link Matricula}.
 *
 * <p>Apenas matrículas {@link #ATIVA} são consideradas no processamento do
 * evento de turma; demais status são ignorados pela busca.</p>
 *
 * @author Equipe matricula-process
 */
public enum StatusMatricula {

    /** Matrícula ativa — elegível ao processamento. */
    ATIVA,

    /** Matrícula cancelada — ignorada no processamento. */
    CANCELADA,

    /** Matrícula trancada — ignorada no processamento. */
    TRANCADA
}
