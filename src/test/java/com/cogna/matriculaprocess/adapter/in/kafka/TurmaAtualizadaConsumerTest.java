package com.cogna.matriculaprocess.adapter.in.kafka;

import com.cogna.matriculaprocess.application.dto.TurmaAtualizadaCommand;
import com.cogna.matriculaprocess.application.port.in.ProcessarTurmaAtualizadaUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TurmaAtualizadaConsumerTest {

    @Mock
    private ProcessarTurmaAtualizadaUseCase useCase;

    private TurmaAtualizadaConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new TurmaAtualizadaConsumer(useCase, new ObjectMapper());
    }

    private ConsumerRecord<String, String> record(String payload) {
        return new ConsumerRecord<>("turma-atualizada", 0, 0L, null, payload);
    }

    @Test
    @DisplayName("converte o evento JSON em comando e delega ao caso de uso")
    void consomeEventoValido() {
        String payload = """
                {
                  "businessKey": "GRAD/ENG/BACH/PRESENCIAL/NOTURNO/UNIT-SP-01",
                  "turma": {
                    "codigo": "T2026-001",
                    "diasDaSemana": ["SEGUNDA", "QUARTA", "SEXTA"],
                    "horarioInicio": "19:00",
                    "horarioFim": "22:30",
                    "vagas": 40
                  },
                  "cicloId": 20261
                }
                """;

        consumer.consumir(record(payload));

        ArgumentCaptor<TurmaAtualizadaCommand> captor =
                ArgumentCaptor.forClass(TurmaAtualizadaCommand.class);
        verify(useCase).processar(captor.capture());

        TurmaAtualizadaCommand command = captor.getValue();
        assertThat(command.businessKey()).isEqualTo("GRAD/ENG/BACH/PRESENCIAL/NOTURNO/UNIT-SP-01");
        assertThat(command.codigoTurma()).isEqualTo("T2026-001");
        assertThat(command.diasDaSemana()).containsExactly("SEGUNDA", "QUARTA", "SEXTA");
        assertThat(command.cicloId()).isEqualTo(20261L);
        assertThat(command.vagas()).isEqualTo(40);
    }

    @Test
    @DisplayName("payload com JSON inválido lança MensagemInvalidaException (vai para DLT)")
    void payloadInvalido() {
        assertThatThrownBy(() -> consumer.consumir(record("{nao-e-json")))
                .isInstanceOf(MensagemInvalidaException.class);
        verifyNoInteractions(useCase);
    }

    @Test
    @DisplayName("evento sem campos obrigatórios lança MensagemInvalidaException")
    void eventoSemCamposObrigatorios() {
        assertThatThrownBy(() -> consumer.consumir(record("{\"businessKey\": \"BK\"}")))
                .isInstanceOf(MensagemInvalidaException.class);
        verifyNoInteractions(useCase);
    }
}
