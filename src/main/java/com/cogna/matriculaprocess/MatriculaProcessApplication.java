package com.cogna.matriculaprocess;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do microsserviço <strong>matricula-process</strong>.
 *
 * <p>O serviço segue uma arquitetura <em>event-driven</em>: consome eventos do
 * tópico Kafka {@code turma-atualizada}, valida a vigência do ciclo letivo em um
 * serviço REST externo, aplica a regra de comparação dos dias da semana,
 * persiste o resultado no MongoDB e publica novos eventos no tópico
 * {@code matricula-atualizada}.</p>
 *
 * <p>A organização do código segue o estilo de Ports &amp; Adapters
 * (arquitetura hexagonal): o domínio e os casos de uso não dependem de
 * frameworks de infraestrutura, que ficam isolados nos adaptadores.</p>
 *
 * @author Equipe matricula-process
 * @see com.cogna.matriculaprocess.application.service.ProcessarTurmaAtualizadaService
 */
@SpringBootApplication
public class MatriculaProcessApplication {

    /**
     * Inicializa o contexto Spring Boot e sobe os consumidores/produtores Kafka,
     * a conexão com o MongoDB e o cliente REST de ciclos.
     *
     * @param args argumentos de linha de comando repassados ao Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(MatriculaProcessApplication.class, args);
    }
}
