# OS-101 — Caminhos e códigos para implementação manual

Crie os arquivos abaixo na ordem indicada. Os códigos deste guia ainda não foram aplicados ao projeto.

Todos os caminhos são relativos à raiz do projeto:
`/Users/alinesilva/Desktop/UNIESP/P5/BackEndAvancado/ex1`.

O código reaproveita `ClienteEntity`, `ClienteRepository`, `EquipamentoEntity`, as exceções e o `GlobalExceptionHandler` existentes.

## 1. Migration de ordens

**Criar:** `src/main/resources/db/migration/V3__create_table_ordens.sql`

A sequência gera um sufixo único, inclusive em aberturas simultâneas. O protocolo terá o formato `OS-20261014-0001`; após 9999, o sufixo cresce sem truncamento. A sequência não reinicia a cada dia.

```sql
CREATE SEQUENCE protocolo_os_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE ordens
(
    id                 BIGSERIAL    NOT NULL,
    numero_protocolo   VARCHAR(40)  NOT NULL,
    cliente_id         BIGINT       NOT NULL,
    equipamento_id     BIGINT       NOT NULL,
    descricao_problema TEXT         NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    data_abertura      TIMESTAMP    NOT NULL,

    CONSTRAINT pk_ordens PRIMARY KEY (id),
    CONSTRAINT uk_ordens_numero_protocolo UNIQUE (numero_protocolo),
    CONSTRAINT fk_ordens_cliente FOREIGN KEY (cliente_id)
        REFERENCES clientes (id),
    CONSTRAINT fk_ordens_equipamento FOREIGN KEY (equipamento_id)
        REFERENCES equipamentos (id)
);
```

## 2. Migration de histórico

**Criar:** `src/main/resources/db/migration/V4__create_table_historico_status.sql`

```sql
CREATE TABLE historico_status
(
    id            BIGSERIAL   NOT NULL,
    ordem_id      BIGINT      NOT NULL,
    status        VARCHAR(30) NOT NULL,
    data_registro TIMESTAMP   NOT NULL,
    responsavel   VARCHAR(50) NOT NULL,

    CONSTRAINT pk_historico_status PRIMARY KEY (id),
    CONSTRAINT fk_historico_status_ordem FOREIGN KEY (ordem_id)
        REFERENCES ordens (id)
);
```

Não modificar as migrations V1 e V2 existentes.

## 3. Enum de status

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/ordem/enums/StatusOrdem.java`

```java
package br.edu.uniesp.assistencia.internal.ordem.enums;

public enum StatusOrdem {
    RECEBIDA
}
```

## 4. Entidade de ordem

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/ordem/entity/OrdemEntity.java`

```java
package br.edu.uniesp.assistencia.internal.ordem.entity;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.internal.ordem.enums.StatusOrdem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "ordens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_protocolo", nullable = false, unique = true, length = 40)
    private String numeroProtocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipamento_id", nullable = false)
    private EquipamentoEntity equipamento;

    @Column(name = "descricao_problema", nullable = false, columnDefinition = "TEXT")
    private String descricaoProblema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdem status;

    @Column(name = "data_abertura", nullable = false)
    private LocalDateTime dataAbertura;

    public OrdemEntity(String numeroProtocolo, ClienteEntity cliente,
                       EquipamentoEntity equipamento, String descricaoProblema,
                       LocalDateTime dataAbertura) {
        this.numeroProtocolo = numeroProtocolo;
        this.cliente = cliente;
        this.equipamento = equipamento;
        this.descricaoProblema = descricaoProblema;
        this.status = StatusOrdem.RECEBIDA;
        this.dataAbertura = dataAbertura;
    }
}
```

## 5. Entidade de histórico

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/ordem/entity/HistoricoStatusEntity.java`

```java
package br.edu.uniesp.assistencia.internal.ordem.entity;

import br.edu.uniesp.assistencia.internal.ordem.enums.StatusOrdem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "historico_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistoricoStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordem_id", nullable = false)
    private OrdemEntity ordem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusOrdem status;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    @Column(nullable = false, length = 50)
    private String responsavel;

    public HistoricoStatusEntity(OrdemEntity ordem, StatusOrdem status,
                                 LocalDateTime dataRegistro, String responsavel) {
        this.ordem = ordem;
        this.status = status;
        this.dataRegistro = dataRegistro;
        this.responsavel = responsavel;
    }
}
```

## 6. Repositório de equipamentos

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/equipamento/repository/EquipamentoRepository.java`

```java
package br.edu.uniesp.assistencia.internal.equipamento.repository;

import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipamentoRepository extends JpaRepository<EquipamentoEntity, Long> {
}
```

## 7. Repositório de ordens

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/ordem/repository/OrdemRepository.java`

```java
package br.edu.uniesp.assistencia.internal.ordem.repository;

import br.edu.uniesp.assistencia.internal.ordem.entity.OrdemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrdemRepository extends JpaRepository<OrdemEntity, Long> {

    @Query(value = "SELECT nextval('protocolo_os_seq')", nativeQuery = true)
    Long proximoNumeroProtocolo();
}
```

## 8. Repositório de histórico

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/ordem/repository/HistoricoStatusRepository.java`

```java
package br.edu.uniesp.assistencia.internal.ordem.repository;

import br.edu.uniesp.assistencia.internal.ordem.entity.HistoricoStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatusEntity, Long> {
}
```

## 9. DTO de entrada

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/ordem/dto/CriarOrdemRequest.java`

```java
package br.edu.uniesp.assistencia.internal.ordem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CriarOrdemRequest(
        @NotNull(message = "O clienteId é obrigatório.")
        Long clienteId,

        @NotNull(message = "O equipamentoId é obrigatório.")
        Long equipamentoId,

        @NotBlank(message = "A descricaoProblema é obrigatória.")
        String descricaoProblema
) {
}
```

Os IDs são números: campos ausentes ou nulos são tratados por `@NotNull`. `@NotBlank` trata descrição nula, vazia ou apenas com espaços.

## 10. DTO de resposta

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/ordem/dto/CriarOrdemResponse.java`

```java
package br.edu.uniesp.assistencia.internal.ordem.dto;

import br.edu.uniesp.assistencia.internal.ordem.enums.StatusOrdem;

import java.time.LocalDateTime;

public record CriarOrdemResponse(
        Long id,
        String numeroProtocolo,
        StatusOrdem status,
        LocalDateTime dataAbertura
) {
}
```

## 11. Serviço de abertura

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/ordem/service/OrdemService.java`

```java
package br.edu.uniesp.assistencia.internal.ordem.service;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.cliente.repository.ClienteRepository;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.internal.equipamento.repository.EquipamentoRepository;
import br.edu.uniesp.assistencia.internal.ordem.dto.CriarOrdemRequest;
import br.edu.uniesp.assistencia.internal.ordem.dto.CriarOrdemResponse;
import br.edu.uniesp.assistencia.internal.ordem.entity.HistoricoStatusEntity;
import br.edu.uniesp.assistencia.internal.ordem.entity.OrdemEntity;
import br.edu.uniesp.assistencia.internal.ordem.repository.HistoricoStatusRepository;
import br.edu.uniesp.assistencia.internal.ordem.repository.OrdemRepository;
import br.edu.uniesp.assistencia.shared.exception.RecursoNaoEncontradoException;
import br.edu.uniesp.assistencia.shared.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OrdemService {

    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final OrdemRepository ordemRepository;
    private final HistoricoStatusRepository historicoStatusRepository;

    @Transactional
    public CriarOrdemResponse abrir(CriarOrdemRequest request) {
        ClienteEntity cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente", request.clienteId()));

        EquipamentoEntity equipamento = equipamentoRepository.findById(request.equipamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Equipamento", request.equipamentoId()));

        if (!cliente.isAtivo()) {
            throw new RegraNegocioException(
                    "Não é permitido abrir ordem de serviço para cliente inativo.");
        }

        if (!equipamento.getCliente().getId().equals(cliente.getId())) {
            throw new RegraNegocioException(
                    "O equipamento informado não pertence ao cliente informado.");
        }

        LocalDateTime dataAbertura = LocalDateTime.now();
        String dataProtocolo = dataAbertura.format(DateTimeFormatter.BASIC_ISO_DATE);
        Long sequencial = ordemRepository.proximoNumeroProtocolo();
        String numeroProtocolo = String.format(
                Locale.ROOT, "OS-%s-%04d", dataProtocolo, sequencial);

        OrdemEntity ordem = new OrdemEntity(
                numeroProtocolo,
                cliente,
                equipamento,
                request.descricaoProblema(),
                dataAbertura);

        ordem = ordemRepository.save(ordem);

        HistoricoStatusEntity historico = new HistoricoStatusEntity(
                ordem, ordem.getStatus(), dataAbertura, "SISTEMA");
        historicoStatusRepository.save(historico);

        return new CriarOrdemResponse(
                ordem.getId(),
                ordem.getNumeroProtocolo(),
                ordem.getStatus(),
                ordem.getDataAbertura());
    }
}
```

`@Transactional` faz a OS e o histórico serem persistidos juntos: uma falha de persistência desfaz ambos. A sequência pode ter lacunas após rollback; isso não compromete a unicidade.

As mensagens de 422 acima foram propostas porque o enunciado não fornece os textos. A data/hora usa o fuso configurado no servidor.

## 12. Controller

**Criar:** `src/main/java/br/edu/uniesp/assistencia/internal/ordem/controller/OrdemController.java`

```java
package br.edu.uniesp.assistencia.internal.ordem.controller;

import br.edu.uniesp.assistencia.internal.ordem.dto.CriarOrdemRequest;
import br.edu.uniesp.assistencia.internal.ordem.dto.CriarOrdemResponse;
import br.edu.uniesp.assistencia.internal.ordem.service.OrdemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/ordens")
@RequiredArgsConstructor
public class OrdemController {

    private final OrdemService ordemService;

    @PostMapping
    public ResponseEntity<CriarOrdemResponse> abrir(
            @Valid @RequestBody CriarOrdemRequest request) {
        CriarOrdemResponse response = ordemService.abrir(request);
        URI location = URI.create("/ordens/" + response.id());
        return ResponseEntity.created(location).body(response);
    }
}
```

## 13. Ajuste para IDs enviados em branco ou JSON inválido

**Ajustar:** `src/main/java/br/edu/uniesp/assistencia/config/exception/GlobalExceptionHandler.java`

Manter os três handlers existentes. Acrescentar o import abaixo e o método dentro da classe para padronizar erros de leitura do JSON como 400. Isso cobre, por exemplo, um ID enviado como texto incompatível com `Long`.

```java
import org.springframework.http.converter.HttpMessageNotReadableException;
```

```java
@ExceptionHandler(HttpMessageNotReadableException.class)
public ProblemDetail tratarJsonInvalido(
        HttpMessageNotReadableException exception,
        HttpServletRequest request) {
    ProblemDetail problema = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "JSON inválido. Informe clienteId e equipamentoId como números e descricaoProblema como texto.");

    problema.setTitle("Dados de requisição inválidos");
    problema.setInstance(URI.create(request.getRequestURI()));
    return problema;
}
```

Campos ausentes/nulos e descrição em branco passam pelo handler de validação já existente, com a propriedade `erros` por campo. JSON que não pode ser convertido recebe o erro de leitura acima.

## 14. Conferir manualmente

Após criar os arquivos, iniciar a aplicação com a configuração de banco utilizada no projeto. O Flyway aplicará V3 e V4. Usar IDs reais de um cliente ativo e de um equipamento pertencente a ele:

```http
POST http://localhost:8080/ordens
Content-Type: application/json
```

```json
{
  "clienteId": 1,
  "equipamentoId": 2,
  "descricaoProblema": "Notebook não liga após queda de energia elétrica."
}
```

Esperado: **201**, `Location: /ordens/{id}` e resposta com `id`, `numeroProtocolo`, `status: "RECEBIDA"` e `dataAbertura`.

Conferir o registro inicial no banco:

```sql
SELECT id, numero_protocolo, status, data_abertura
FROM ordens
ORDER BY id DESC;

SELECT ordem_id, status, responsavel, data_registro
FROM historico_status
ORDER BY id DESC;
```

- Cliente inexistente ou equipamento inexistente: **404** com ProblemDetail.
- Cliente inativo ou equipamento de outro cliente: **422** com a mensagem da regra.
- Corpo `{}`: **400**, com erros de `clienteId`, `equipamentoId` e `descricaoProblema`.
- Descrição `""` ou `"   "`: **400**, com erro de `descricaoProblema`.
- Duas aberturas válidas: protocolos diferentes e um histórico RECEBIDA/SISTEMA para cada OS.

Não é necessário alterar `pom.xml`, as entidades de cliente/equipamento nem criar novos endpoints para atender a esta atividade.
