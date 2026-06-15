package com.cogna.matriculaprocess.application.port.in;

import com.cogna.matriculaprocess.application.dto.TurmaAtualizadaCommand;

public interface ProcessarTurmaAtualizadaUseCase {

    void processar(TurmaAtualizadaCommand command);
}
