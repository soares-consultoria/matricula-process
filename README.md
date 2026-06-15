# matricula-process

> **Tipo de projeto:** Aplicação / Microsserviço (Spring Boot 3.x · Java 21 · Gradle)

## Visão Geral

O **matricula-process** é um microsserviço *event-driven* que mantém os dias da semana das matrículas ativas sincronizados com as atualizações de turma, dentro de um ecossistema de processamento de dados acadêmicos.

**Objetivo de negócio:** quando a grade de uma turma muda (ex.: passa a ter aula também na sexta-feira), todas as matrículas ativas daquela turma precisam refletir os novos dias — desde que o ciclo letivo esteja vigente. O serviço automatiza essa propagação e notifica os sistemas downstream sobre cada matrícula alterada.

**Objetivos técnicos:**

- Integrar-se ao ecossistema via Kafka (consumo e publicação de eventos).
- Validar regras de negócio consultando um serviço REST externo (ciclos).
- Persistir com eficiência em MongoDB, suportando milhares de matrículas por chave de negócio.
- Garantir resiliência (retry/DLT) e rastreabilidade (logs estruturados com MDC).

O serviço **não expõe uma API REST de negócio**: seu "contrato público" são os tópicos Kafka que consome e publica (ver [Referência da API](#referência-da-api)).

## Fluxo

```
 Kafka (turma-atualizada)
        │
        ▼
 ┌─────────────────────┐     GET /api/ciclos/{id}     ┌──────────────┐
 │  matricula-process  │ ───────────────────────────► │  Ciclo API   │
 │                     │ ◄─────────────────────────── │  (WireMock)  │
 └─────────────────────┘                              └──────────────┘
        │      │
        │      └── busca/atualiza matrículas ATIVAS ──► MongoDB (matriculas)
        ▼
 Kafka (matricula-atualizada)   [1 evento por matrícula atualizada]
```

1. Consome o evento do tópico `turma-atualizada`.
2. Consulta a API de ciclos. O ciclo é **vigente** quando `ativo == true` **e** `dataInicioCaptura <= hoje < dataFimCaptura`.
3. Ciclo não vigente ou inexistente (404) → evento logado e descartado.
4. Ciclo vigente → busca as matrículas `ATIVA` do mesmo `businessKey` e compara os `diasDaSemana` (comparação por conjunto, ignorando ordem).
5. Dias diferentes → atualiza a matrícula no MongoDB e publica um evento em `matricula-atualizada`.

## Arquitetura

Hexagonal (Ports & Adapters):

```
src/main/java/com/cogna/matriculaprocess
├── domain/model/          # Entidades e regras puras (Matricula, Ciclo, Turma)
├── application/
│   ├── dto/               # Comandos e eventos da aplicação
│   ├── port/in|out/       # Portas (interfaces)
│   └── service/           # Caso de uso ProcessarTurmaAtualizadaService
├── adapter/
│   ├── in/kafka/          # Consumer do tópico turma-atualizada
│   └── out/
│       ├── rest/          # Cliente da API de ciclos (RestClient)
│       ├── mongo/         # Persistência (MongoTemplate, stream + update direcionado)
│       └── kafka/         # Producer do tópico matricula-atualizada
└── config/                # Tópicos, error handler (retry/DLT), RestClient, Clock
```

O domínio e o caso de uso não dependem de Kafka, Mongo ou HTTP — apenas das portas. O `Clock` é injetado para tornar a regra de vigência testável.

### Decisões de projeto

- **Escala**: a busca de matrículas usa `MongoTemplate.stream` (cursor) — milhares de matrículas por `businessKey` são processadas sem carregar tudo em memória. A atualização é um `updateFirst` direcionado em `turma.diasDaSemana` (não regrava o documento inteiro).
- **Índice**: índice composto `{businessKey, status}` criado pela aplicação na inicialização (`auto-index-creation`).
- **Tópicos**: criados pela aplicação na inicialização via `KafkaAdmin` (`turma-atualizada`, `matricula-atualizada`, `turma-atualizada.DLT`).
- **Resiliência**: falhas transitórias (ex.: 5xx da API de ciclos) fazem retry com backoff exponencial (3 tentativas); esgotado o retry, a mensagem vai para a DLT `turma-atualizada.DLT`. Payload inválido vai direto para a DLT, sem retry. **Atenção**: 404 e ciclo não vigente *não* são erros — o evento é descartado por regra de negócio.
- **Ordenação**: eventos de saída usam `businessKey` como chave de partição.
- **Logs**: JSON estruturado (logstash-encoder) com MDC (`correlationId`, `businessKey`, `cicloId`, partição/offset). Com o profile `local`, logs em texto plano.
- **Comparação de dias**: por conjunto (ordem e duplicidade irrelevantes): `["QUARTA","SEGUNDA"]` ≡ `["SEGUNDA","QUARTA"]`.

## Requisitos

- Docker + Docker Compose (execução completa)
- Java 21 (apenas para rodar testes/build localmente — o build também pode ser feito dentro do Docker)

## Como executar

### Subir tudo com Docker Compose

```bash
docker compose up --build
```

Sobe MongoDB (com a massa de dados inicial), Kafka (KRaft, sem Zookeeper), WireMock (mock da API de ciclos na porta 8089) e a aplicação (porta 8080). A aplicação cria tópicos e índices na inicialização.

### Testar o fluxo ponta a ponta

Publique o evento de exemplo no tópico `turma-atualizada`:

```bash
docker exec -i matricula-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic turma-atualizada <<'EOF'
{"businessKey":"GRAD/ENG/BACH/PRESENCIAL/NOTURNO/UNIT-SP-01","turma":{"codigo":"T2026-001","diasDaSemana":["SEGUNDA","QUARTA","SEXTA"],"horarioInicio":"19:00","horarioFim":"22:30","vagas":40},"cicloId":20261}
EOF
```

Consuma o resultado em `matricula-atualizada`:

```bash
docker exec -i matricula-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic matricula-atualizada --from-beginning
```

Resultado esperado com a massa inicial:

| Documento | Situação | Comportamento |
|---|---|---|
| ALU-001 (SEG, QUA) | dias diferentes | atualizado + evento publicado |
| ALU-002 (SEG, QUA, SEX) | dias iguais | nada |
| ALU-003 (CANCELADA) | não é ATIVA | ignorado |
| ALU-004 (outro businessKey) | não retornado pela busca | intacto |

Verifique no Mongo:

```bash
docker exec -it matricula-mongodb mongosh matriculadb \
  --eval 'db.matriculas.find({}, {alunoId: 1, status: 1, "turma.diasDaSemana": 1}).toArray()'
```

Cenários negativos (evento descartado, nada publicado): repita o produce trocando `cicloId` por `20262` (inativo), `20252` (expirado) ou `99999` (404).

### Rodar os testes

```bash
./gradlew test
```

### Rodar a aplicação localmente (fora do Docker)

Suba só as dependências e rode a app com o profile `local` (logs legíveis):

```bash
docker compose up -d mongodb kafka ciclo-api
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Os defaults locais já apontam para `localhost` (`Mongo 27017`, `Kafka 9094`, `Ciclo API 8089`).

## Configuração (variáveis de ambiente)

| Variável | Default | Descrição |
|---|---|---|
| `CICLO_API_URL` | `http://localhost:8089` | URL base da API de ciclos (`GET /api/ciclos/{id}`) |
| `MONGODB_URI` | `mongodb://localhost:27017/matriculadb` | URI do MongoDB |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9094` | Bootstrap servers do Kafka |
| `SERVER_PORT` | `8080` | Porta HTTP (actuator) |

> Na avaliação, basta apontar `CICLO_API_URL` para o serviço real — o contrato é o mesmo do mock.

## Mock da API de ciclos (WireMock)

| Requisição | Resposta |
|---|---|
| `GET /api/ciclos/20261` | 200 — ativo, captura 2026-01-15 → 2026-07-01 (vigente) |
| `GET /api/ciclos/20262` | 200 — `ativo: false` |
| `GET /api/ciclos/20252` | 200 — ativo, janela de 2025 (expirado) |
| `GET /api/ciclos/{qualquer outro}` | 404 |

Mapeamentos em [`wiremock/mappings/`](wiremock/mappings/).

## Referência da API

Por ser um serviço *event-driven*, o contrato principal é assíncrono (tópicos Kafka). Não há endpoints REST de negócio nem Swagger/OpenAPI — os únicos endpoints HTTP são os do Actuator (operacionais).

### Contrato de mensageria (Kafka)

| Tópico | Direção | Descrição |
|---|---|---|
| `turma-atualizada` | consome | Evento de entrada com a turma atualizada e o `cicloId` a validar |
| `matricula-atualizada` | publica | Um evento por matrícula efetivamente atualizada |
| `turma-atualizada.DLT` | publica | Dead Letter Topic — mensagens inválidas ou que esgotaram o retry |

**Entrada — `turma-atualizada`:**

```json
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
```

**Saída — `matricula-atualizada`:**

```json
{
  "matriculaId": "64a1b2c3d4e5f6a7b8c9d0e1",
  "alunoId": "ALU-001",
  "businessKey": "GRAD/ENG/BACH/PRESENCIAL/NOTURNO/UNIT-SP-01",
  "cicloId": 20261,
  "diasDaSemanaAnterior": ["SEGUNDA", "QUARTA"],
  "diasDaSemanaNovo": ["SEGUNDA", "QUARTA", "SEXTA"],
  "dataAtualizacao": "2026-06-15T10:30:00"
}
```

### Serviço REST consumido (API de ciclos)

O serviço **consome** (não expõe) o endpoint externo abaixo, configurável via `CICLO_API_URL`:

```
GET {CICLO_API_URL}/api/ciclos/{cicloId}
→ 200 {"id": 20261, "ativo": true, "dataInicioCaptura": "2026-01-15", "dataFimCaptura": "2026-07-01"}
→ 404  (ciclo não encontrado → evento descartado)
```

### Endpoints HTTP (Actuator)

| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Status de saúde da aplicação |
| `GET /actuator/info` | Informações da aplicação |
| `GET /actuator/metrics` | Métricas operacionais |

## Observabilidade

- Health check: `GET http://localhost:8080/actuator/health`
- Logs JSON com MDC para rastreabilidade por evento (`correlationId`, `businessKey`, `cicloId`, `topic`, `partition`, `offset`)

## Geração da Documentação (Javadoc)

A documentação técnica detalhada do código-fonte pode ser gerada utilizando o Javadoc padrão do ecossistema Java.

Para gerar a documentação localmente, execute o comando correspondente à ferramenta de build do projeto na raiz do repositório:

```bash
# Gerar o Javadoc (Gradle)
./gradlew javadoc

# Atalho equivalente
./gradlew docs
```

A documentação é gerada na pasta [`documentacao/`](documentacao/) na raiz do projeto (e não no diretório padrão `build/docs/javadoc`). Após a geração, abra o arquivo `documentacao/index.html` no navegador:

```bash
open documentacao/index.html      # macOS
xdg-open documentacao/index.html  # Linux
```

A task está configurada com encoding `UTF-8` e com o *doclint* desativado (`-Xdoclint:none`), de modo que avisos de documentação ausente em métodos não mapeados **não** interrompem o build.
