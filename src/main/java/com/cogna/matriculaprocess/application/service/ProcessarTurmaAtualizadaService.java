package com.cogna.matriculaprocess.application.service;

import com.cogna.matriculaprocess.application.dto.MatriculaAtualizadaEvento;
import com.cogna.matriculaprocess.application.dto.TurmaAtualizadaCommand;
import com.cogna.matriculaprocess.application.port.in.ProcessarTurmaAtualizadaUseCase;
import com.cogna.matriculaprocess.application.port.out.ConsultarCicloPort;
import com.cogna.matriculaprocess.application.port.out.MatriculaPort;
import com.cogna.matriculaprocess.application.port.out.PublicarMatriculaAtualizadaPort;
import com.cogna.matriculaprocess.domain.model.Ciclo;
import com.cogna.matriculaprocess.domain.model.Matricula;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class ProcessarTurmaAtualizadaService implements ProcessarTurmaAtualizadaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarTurmaAtualizadaService.class);

    private final ConsultarCicloPort consultarCicloPort;
    private final MatriculaPort matriculaPort;
    private final PublicarMatriculaAtualizadaPort publicarPort;
    private final Clock clock;

    public ProcessarTurmaAtualizadaService(ConsultarCicloPort consultarCicloPort,
                                           MatriculaPort matriculaPort,
                                           PublicarMatriculaAtualizadaPort publicarPort,
                                           Clock clock) {
        this.consultarCicloPort = consultarCicloPort;
        this.matriculaPort = matriculaPort;
        this.publicarPort = publicarPort;
        this.clock = clock;
    }

    @Override
    public void processar(TurmaAtualizadaCommand command) {
        Optional<Ciclo> ciclo = consultarCicloPort.buscarPorId(command.cicloId());

        if (ciclo.isEmpty()) {
            log.warn("Evento descartado: ciclo {} não encontrado (404). businessKey={}",
                    command.cicloId(), command.businessKey());
            return;
        }

        LocalDate hoje = LocalDate.now(clock);
        if (!ciclo.get().vigente(hoje)) {
            log.warn("Evento descartado: ciclo {} não vigente em {} (ativo={}, captura={} a {}). businessKey={}",
                    command.cicloId(), hoje, ciclo.get().ativo(),
                    ciclo.get().dataInicioCaptura(), ciclo.get().dataFimCaptura(), command.businessKey());
            return;
        }

        AtomicLong avaliadas = new AtomicLong();
        AtomicLong atualizadas = new AtomicLong();

        try (Stream<Matricula> matriculas = matriculaPort.buscarAtivasPorBusinessKey(command.businessKey())) {
            matriculas.forEach(matricula -> {
                avaliadas.incrementAndGet();
                if (!matricula.mesmosDias(command.diasDaSemana())) {
                    atualizar(matricula, command);
                    atualizadas.incrementAndGet();
                }
            });
        }

        log.info("Processamento concluído. businessKey={} cicloId={} matriculasAvaliadas={} matriculasAtualizadas={}",
                command.businessKey(), command.cicloId(), avaliadas.get(), atualizadas.get());
    }

    private void atualizar(Matricula matricula, TurmaAtualizadaCommand command) {
        matriculaPort.atualizarDiasDaSemana(matricula.id(), command.diasDaSemana());

        publicarPort.publicar(new MatriculaAtualizadaEvento(
                matricula.id(),
                matricula.alunoId(),
                matricula.businessKey(),
                command.cicloId(),
                matricula.turma().diasDaSemana(),
                command.diasDaSemana(),
                LocalDateTime.now(clock)));

        log.info("Matrícula atualizada. matriculaId={} alunoId={} diasAnterior={} diasNovo={}",
                matricula.id(), matricula.alunoId(), matricula.turma().diasDaSemana(), command.diasDaSemana());
    }
}
