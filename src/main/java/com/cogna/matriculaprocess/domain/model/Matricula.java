package com.cogna.matriculaprocess.domain.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * Matrícula de um aluno em uma turma, modelo central do domínio.
 *
 * <p>É a entidade comparada contra o evento de turma recebido: quando os dias da
 * semana da matrícula divergem dos novos dias da turma, a matrícula deve ser
 * atualizada e um evento de saída publicado.</p>
 *
 * @param id            identificador único da matrícula (ObjectId do MongoDB)
 * @param alunoId       identificador do aluno
 * @param businessKey   chave de negócio que agrupa matrículas equivalentes
 * @param status        situação da matrícula (ex.: {@link StatusMatricula#ATIVA})
 * @param turma         dados da turma associada à matrícula
 * @param cicloId       identificador do ciclo letivo
 * @param dataMatricula data/hora em que a matrícula foi realizada
 * @author Equipe matricula-process
 * @see StatusMatricula
 * @see Turma
 */
public record Matricula(
        String id,
        String alunoId,
        String businessKey,
        StatusMatricula status,
        Turma turma,
        Long cicloId,
        LocalDateTime dataMatricula) {

    /**
     * Compara os dias da semana atuais da matrícula com os novos dias da turma.
     *
     * <p>A comparação ignora a ordem e eventuais duplicidades (semântica de
     * conjunto). Valores nulos — tanto na turma quanto no parâmetro — são
     * tratados como lista vazia.</p>
     *
     * @param novosDias novos dias da semana provenientes do evento de turma
     * @return {@code true} se os dias forem equivalentes (nada a fazer);
     *         {@code false} se forem diferentes (matrícula deve ser atualizada)
     */
    public boolean mesmosDias(List<String> novosDias) {
        List<String> diasAtuais = turma != null && turma.diasDaSemana() != null
                ? turma.diasDaSemana()
                : List.of();
        return new HashSet<>(diasAtuais).equals(new HashSet<>(novosDias != null ? novosDias : List.of()));
    }
}
