# OS-101 — Abertura de Ordem de Serviço

## O que já existe

- Cliente com campo `ativo` e repositório.
- Equipamento associado ao cliente.
- Exceções e tratamento com `ProblemDetail` para 404, 422 e validação 400.
- Dependências necessárias: JPA, Validation e Flyway.

## O que precisa ser feito

### 1. Banco e entidades

- [ ] Criar migration e entidade de ordem com: `id`, `numeroProtocolo` único, cliente, equipamento, `descricaoProblema`, `status` e `dataAbertura`.
- [ ] Criar migration e entidade para `historico_status` com: ordem, status, data/hora e responsável.
- [ ] Criar enum de status com `RECEBIDA`.
- [ ] Criar repositórios de ordem, histórico e equipamento (este ainda não existe).

### 2. Entrada e saída da API

- [ ] Criar DTO de entrada: `clienteId` e `equipamentoId` com `@NotNull`; `descricaoProblema` com `@NotBlank`.
- [ ] Criar DTO de resposta: `id`, `numeroProtocolo`, `status` e `dataAbertura`.

### 3. Serviço de abertura

- [ ] Buscar cliente e equipamento. Se algum não existir, lançar `RecursoNaoEncontradoException` (404).
- [ ] Se o cliente estiver inativo, lançar `RegraNegocioException` (422).
- [ ] Se o equipamento não pertencer ao cliente informado, lançar `RegraNegocioException` (422).
- [ ] Gerar protocolo amigável e único, no formato `OS-YYYYMMDD-XXXX`. Garantir unicidade também no banco.
- [ ] Definir automaticamente status `RECEBIDA` e data/hora de abertura.
- [ ] Salvar a OS e o primeiro histórico com status `RECEBIDA` e responsável `SISTEMA`, na mesma transação (`@Transactional`).

### 4. Controller

- [ ] Criar `POST /ordens`, recebendo o DTO com `@Valid @RequestBody`.
- [ ] Retornar **201 Created**, cabeçalho **Location: /ordens/{id}** e o DTO de resposta.
- [ ] Reaproveitar o `GlobalExceptionHandler` existente para os erros 400, 404 e 422.

## Conferência final

- [ ] Dados válidos → 201, Location correto, protocolo, status RECEBIDA e data de abertura.
- [ ] Primeiro histórico registrado como RECEBIDA/SISTEMA.
- [ ] Cliente ou equipamento inexistente → 404 com ProblemDetail.
- [ ] Cliente inativo ou equipamento de outro cliente → 422.
- [ ] Campos obrigatórios ausentes/nulos e descrição vazia ou em branco → 400 com erros por campo.
- [ ] Aberturas diferentes geram protocolos únicos.

**Observação:** o enunciado não informa os textos das mensagens de 422. Usar mensagens claras para cliente inativo e equipamento de outro cliente, ou o texto definido pelo professor.
