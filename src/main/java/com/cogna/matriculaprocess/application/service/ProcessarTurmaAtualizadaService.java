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

/**
 * Implementação do caso de uso de processamento de turma atualizada.
 *
 * <p>Orquestra as regras de negócio do serviço, na seguinte ordem:</p>
 * <ol>
 *     <li>Consulta o ciclo no serviço externo via {@link ConsultarCicloPort}.</li>
 *     <li>Se o ciclo não existir (404) ou não estiver vigente, o evento é
 *     logado e descartado — nada é persistido nem publicado.</li>
 *     <li>Se vigente, busca as matrículas ATIVAS do {@code businessKey} e, para
 *     cada uma cujos dias divergem dos novos dias da turma, atualiza a
 *     persistência e publica o evento {@code matricula-atualizada}.</li>
 * </ol>
 *
 * <p>O {@link Clock} é injetado para tornar a verificação de vigência e o
 * carimbo de data/hora testáveis de forma determinística.</p>
 *
 * @author Equipe matricula-process
 * @see ProcessarTurmaAtualizadaUseCase
 * @see com.cogna.matriculaprocess.domain.model.Ciclo#vigente(java.time.LocalDate)
 */
@Service
public class ProcessarTurmaAtualizadaService implements ProcessarTurmaAtualizadaUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessarTurmaAtualizadaService.class);

    private final ConsultarCicloPort consultarCicloPort;
    private final MatriculaPort matriculaPort;
    private final PublicarMatriculaAtualizadaPort publicarPort;
    private final Clock clock;

    /**
     * Cria o serviço com suas portas de saída e o relógio.
     *
     * @param consultarCicloPort porta de consulta de ciclos no serviço externo
     * @param matriculaPort      porta de acesso às matrículas persistidas
     * @param publicarPort       porta de publicação do evento de saída
     * @param clock              relógio usado na verificação de vigência e no
     *                           carimbo de {@code dataAtualizacao}
     */
    public ProcessarTurmaAtualizadaService(ConsultarCicloPort consultarCicloPort,
                                           MatriculaPort matriculaPort,
                                           PublicarMatriculaAtualizadaPort publicarPort,
                                           Clock clock) {
        this.consultarCicloPort = consultarCicloPort;
        this.matriculaPort = matriculaPort;
        this.publicarPort = publicarPort;
        this.clock = clock;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Aplica a validação de vigência do ciclo e, quando vigente, percorre as
     * matrículas ATIVAS atualizando apenas as que tiverem dias divergentes.
     * Eventos de ciclos inexistentes ou não vigentes são descartados com log.</p>
     *
     * @param command comando com os dados da turma e o ciclo a validar
     */
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

    /**
     * Persiste os novos dias da matrícula e publica o evento de atualização.
     *
     * @param matricula matrícula com dias divergentes a ser atualizada
     * @param command   comando com os novos dados da turma
     */
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
