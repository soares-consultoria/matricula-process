package com.cogna.matriculaprocess.application.port.in;

import com.cogna.matriculaprocess.application.dto.TurmaAtualizadaCommand;

/**
 * Porta de entrada (driving port) do caso de uso de processamento de turma
 * atualizada.
 *
 * <p>Define o contrato invocado pelos adaptadores de entrada (ex.: o consumidor
 * Kafka) para acionar a regra de negócio, mantendo a camada de aplicação
 * independente da tecnologia de entrega.</p>
 *
 * @author Equipe matricula-process
 * @see com.cogna.matriculaprocess.application.service.ProcessarTurmaAtualizadaService
 */
public interface ProcessarTurmaAtualizadaUseCase {

    /**
     * Processa um evento de turma atualizada aplicando as regras de negócio:
     * valida a vigência do ciclo, busca as matrículas ATIVAS do
     * {@code businessKey} e atualiza/publica as que tiverem dias divergentes.
     *
     * @param command comando com os dados da turma e o ciclo a validar
     */
    void processar(TurmaAtualizadaCommand command);
}
