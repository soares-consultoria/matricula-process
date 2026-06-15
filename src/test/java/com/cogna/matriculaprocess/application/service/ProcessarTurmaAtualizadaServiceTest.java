package com.cogna.matriculaprocess.application.service;

import com.cogna.matriculaprocess.application.dto.MatriculaAtualizadaEvento;
import com.cogna.matriculaprocess.application.dto.TurmaAtualizadaCommand;
import com.cogna.matriculaprocess.application.port.out.ConsultarCicloPort;
import com.cogna.matriculaprocess.application.port.out.MatriculaPort;
import com.cogna.matriculaprocess.application.port.out.PublicarMatriculaAtualizadaPort;
import com.cogna.matriculaprocess.domain.model.Ciclo;
import com.cogna.matriculaprocess.domain.model.Matricula;
import com.cogna.matriculaprocess.domain.model.StatusMatricula;
import com.cogna.matriculaprocess.domain.model.Turma;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessarTurmaAtualizadaServiceTest {

    private static final String BUSINESS_KEY = "GRAD/ENG/BACH/PRESENCIAL/NOTURNO/UNIT-SP-01";
    private static final Long CICLO_ID = 20261L;
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private static final Clock CLOCK_FIXO =
            Clock.fixed(Instant.parse("2026-06-12T13:30:00Z"), ZONE);

    @Mock
    private ConsultarCicloPort consultarCicloPort;

    @Mock
    private MatriculaPort matriculaPort;

    @Mock
    private PublicarMatriculaAtualizadaPort publicarPort;

    private ProcessarTurmaAtualizadaService service;

    @BeforeEach
    void setUp() {
        service = new ProcessarTurmaAtualizadaService(
                consultarCicloPort, matriculaPort, publicarPort, CLOCK_FIXO);
    }

    private TurmaAtualizadaCommand command(List<String> novosDias) {
        return new TurmaAtualizadaCommand(BUSINESS_KEY, "T2026-001", novosDias,
                "19:00", "22:30", 40, CICLO_ID);
    }

    private Ciclo cicloVigente() {
        return new Ciclo(CICLO_ID, true, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 7, 1));
    }

    private Matricula matricula(String id, String alunoId, List<String> dias) {
        return new Matricula(id, alunoId, BUSINESS_KEY, StatusMatricula.ATIVA,
                new Turma("T2026-001", dias, "19:00", "22:30"),
                CICLO_ID, LocalDateTime.of(2026, 2, 10, 8, 0));
    }

    @Test
    @DisplayName("ciclo não encontrado (404): descarta sem consultar matrículas nem publicar")
    void cicloNaoEncontrado() {
        when(consultarCicloPort.buscarPorId(CICLO_ID)).thenReturn(Optional.empty());

        service.processar(command(List.of("SEGUNDA", "QUARTA", "SEXTA")));

        verifyNoInteractions(matriculaPort, publicarPort);
    }

    @Test
    @DisplayName("ciclo inativo: descarta o evento")
    void cicloInativo() {
        when(consultarCicloPort.buscarPorId(CICLO_ID)).thenReturn(Optional.of(
                new Ciclo(CICLO_ID, false, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 7, 1))));

        service.processar(command(List.of("SEGUNDA")));

        verifyNoInteractions(matriculaPort, publicarPort);
    }

    @Test
    @DisplayName("ciclo expirado (fora da janela de captura): descarta o evento")
    void cicloExpirado() {
        when(consultarCicloPort.buscarPorId(CICLO_ID)).thenReturn(Optional.of(
                new Ciclo(CICLO_ID, true, LocalDate.of(2025, 1, 15), LocalDate.of(2025, 7, 1))));

        service.processar(command(List.of("SEGUNDA")));

        verifyNoInteractions(matriculaPort, publicarPort);
    }

    @Test
    @DisplayName("ciclo vigente e dias diferentes: atualiza matrícula e publica evento")
    void atualizaEPublicaQuandoDiasDiferentes() {
        List<String> novosDias = List.of("SEGUNDA", "QUARTA", "SEXTA");
        Matricula desatualizada = matricula("id-1", "ALU-001", List.of("SEGUNDA", "QUARTA"));

        when(consultarCicloPort.buscarPorId(CICLO_ID)).thenReturn(Optional.of(cicloVigente()));
        when(matriculaPort.buscarAtivasPorBusinessKey(BUSINESS_KEY))
                .thenReturn(Stream.of(desatualizada));

        service.processar(command(novosDias));

        verify(matriculaPort).atualizarDiasDaSemana("id-1", novosDias);

        ArgumentCaptor<MatriculaAtualizadaEvento> captor =
                ArgumentCaptor.forClass(MatriculaAtualizadaEvento.class);
        verify(publicarPort).publicar(captor.capture());

        MatriculaAtualizadaEvento evento = captor.getValue();
        assertThat(evento.matriculaId()).isEqualTo("id-1");
        assertThat(evento.alunoId()).isEqualTo("ALU-001");
        assertThat(evento.businessKey()).isEqualTo(BUSINESS_KEY);
        assertThat(evento.cicloId()).isEqualTo(CICLO_ID);
        assertThat(evento.diasDaSemanaAnterior()).containsExactly("SEGUNDA", "QUARTA");
        assertThat(evento.diasDaSemanaNovo()).containsExactly("SEGUNDA", "QUARTA", "SEXTA");
        assertThat(evento.dataAtualizacao()).isEqualTo(LocalDateTime.now(CLOCK_FIXO));
    }

    @Test
    @DisplayName("ciclo vigente e dias iguais: não atualiza nem publica")
    void naoFazNadaQuandoDiasIguais() {
        List<String> novosDias = List.of("SEGUNDA", "QUARTA", "SEXTA");

        when(consultarCicloPort.buscarPorId(CICLO_ID)).thenReturn(Optional.of(cicloVigente()));
        when(matriculaPort.buscarAtivasPorBusinessKey(BUSINESS_KEY))
                .thenReturn(Stream.of(matricula("id-2", "ALU-002", novosDias)));

        service.processar(command(novosDias));

        verify(matriculaPort, never()).atualizarDiasDaSemana(anyString(), any());
        verifyNoInteractions(publicarPort);
    }

    @Test
    @DisplayName("dias iguais em ordem diferente são considerados iguais")
    void diasIguaisEmOrdemDiferente() {
        when(consultarCicloPort.buscarPorId(CICLO_ID)).thenReturn(Optional.of(cicloVigente()));
        when(matriculaPort.buscarAtivasPorBusinessKey(BUSINESS_KEY))
                .thenReturn(Stream.of(matricula("id-3", "ALU-003", List.of("SEXTA", "SEGUNDA", "QUARTA"))));

        service.processar(command(List.of("SEGUNDA", "QUARTA", "SEXTA")));

        verify(matriculaPort, never()).atualizarDiasDaSemana(anyString(), any());
        verifyNoInteractions(publicarPort);
    }

    @Test
    @DisplayName("mistura de matrículas: atualiza só as desatualizadas")
    void atualizaApenasDesatualizadas() {
        List<String> novosDias = List.of("SEGUNDA", "QUARTA", "SEXTA");

        when(consultarCicloPort.buscarPorId(CICLO_ID)).thenReturn(Optional.of(cicloVigente()));
        when(matriculaPort.buscarAtivasPorBusinessKey(BUSINESS_KEY)).thenReturn(Stream.of(
                matricula("id-1", "ALU-001", List.of("SEGUNDA", "QUARTA")),
                matricula("id-2", "ALU-002", novosDias)));

        service.processar(command(novosDias));

        verify(matriculaPort).atualizarDiasDaSemana("id-1", novosDias);
        verify(matriculaPort, never()).atualizarDiasDaSemana(org.mockito.ArgumentMatchers.eq("id-2"), any());

        ArgumentCaptor<MatriculaAtualizadaEvento> captor =
                ArgumentCaptor.forClass(MatriculaAtualizadaEvento.class);
        verify(publicarPort).publicar(captor.capture());
        assertThat(captor.getValue().matriculaId()).isEqualTo("id-1");
    }
}
