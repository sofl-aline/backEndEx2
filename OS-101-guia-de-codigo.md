# OS-101 — Guia da implementação

## Fluxo de abertura

1. `OrdemServicoController` recebe `POST /ordens` com `@Valid` e o Record `CriarOrdemServicoRequest`.
2. `OrdemServicoService` busca cliente e equipamento ou lança `RecursoNaoEncontradoException`.
3. O Service captura a data/hora e obtém o próximo valor de `protocolo_os_seq`.
4. O protocolo utiliza a data da abertura e quatro caracteres em base 36 maiúscula.
5. `OrdemServicoEntity` valida cliente ativo e propriedade do equipamento, define `RECEBIDA` e inclui o histórico inicial.
6. O repository salva a OS e o histórico por cascade na mesma transação.
7. `OrdemServicoMapper` produz o Record de resposta; o Controller retorna 201 e Location.

## Contrato

```http
POST /ordens
Content-Type: application/json
```

```json
{
  "clienteId": 1,
  "equipamentoId": 2,
  "descricaoProblema": "Notebook não liga após queda de energia elétrica."
}
```

Resposta 201, com valores ilustrativos:

```json
{
  "id": 1,
  "numeroProtocolo": "OS-20261020-0001",
  "status": "RECEBIDA",
  "dataAbertura": "2026-10-20T20:30:00"
}
```

## Erros

- 400: IDs ausentes/nulos ou descrição ausente/nula/em branco. ProblemDetail inclui `erros`, relacionando campos e mensagens. JSON malformado ou tipos incompatíveis também retornam 400.
- 404: cliente ou equipamento inexistente. ProblemDetail inclui `recurso` e `identificador`.
- 422: `Não é permitido abrir Ordem de Serviço para um cliente inativo.`
- 422: `O equipamento informado não pertence ao cliente indicado.`
- 422: `A capacidade de geração de protocolos foi esgotada.`

## Persistência e testes

As migrations V3 e V4 correspondem aos mapeamentos atuais, incluindo `ordens_servico` e `historico_status`. Não executar os scripts antigos deste guia: os arquivos de implementação já estão no projeto.

Executar `./mvnw test` com Java 21 e Docker disponível. A suíte cria um PostgreSQL temporário com Testcontainers, aplica as migrations e verifica os cenários HTTP, a persistência e a concorrência. O banco configurado para uso local não é alterado pelos testes.
