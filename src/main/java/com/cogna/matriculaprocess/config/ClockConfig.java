package com.cogna.matriculaprocess.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuração do relógio da aplicação.
 *
 * <p>Expor o {@link Clock} como bean permite que regras dependentes de data/hora
 * — como a vigência do ciclo e o carimbo de {@code dataAtualizacao} — sejam
 * testadas de forma determinística (substituindo por um relógio fixo nos
 * testes).</p>
 *
 * @author Equipe matricula-process
 * @see com.cogna.matriculaprocess.application.service.ProcessarTurmaAtualizadaService
 */
@Configuration
public class ClockConfig {

    /**
     * Fornece o relógio do sistema no fuso padrão da JVM.
     *
     * @return relógio do sistema
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
