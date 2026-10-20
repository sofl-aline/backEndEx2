# OS-101 — Abertura de Ordem de Serviço

## Implementação

- `POST /ordens` recebe um Record validado com `clienteId`, `equipamentoId` e `descricaoProblema`.
- O Service busca os recursos e executa a abertura em uma transação.
- O construtor de `OrdemServicoEntity` impede cliente inativo e equipamento de outro cliente, com as mensagens exigidas pelo enunciado.
- A entidade define status `RECEBIDA`, data/hora e histórico inicial com responsável `SISTEMA` e observação `Abertura da ordem de serviço.`.
- O histórico é persistido pelo cascade da OS na mesma transação.
- O mapper converte a entidade para o Record de resposta; o Controller retorna 201 com `Location: /ordens/{id}`.
- O handler retorna ProblemDetail para validação e JSON inválido (400), recursos inexistentes (404) e regras de negócio (422).

## Protocolo

Formato `OS-YYYYMMDD-XXXX`, com sufixo em base 36 maiúscula e quatro caracteres, incluindo zeros à esquerda. A data do protocolo é a mesma da abertura.

A sequência PostgreSQL `protocolo_os_seq` é global, não reinicia por dia e não permite ciclo. Seu intervalo de 1 a 1679615 produz sufixos de `0001` a `ZZZZ`. A coluna possui restrição UNIQUE. A sequência permite aberturas simultâneas sem duplicação; lacunas após rollback são esperadas.

Quando a sequência se esgota, a abertura retorna 422 com a mensagem `A capacidade de geração de protocolos foi esgotada.`. Não há truncamento nem reutilização.

## Banco

V1 e V2 permanecem inalteradas. V3 cria técnicos, OS, histórico e sequência. V4 cria as demais tabelas das entidades existentes. As funcionalidades futuras permanecem no modelo, sem novos endpoints.

## Verificação

Com Java 21 e Docker em execução:

```sh
./mvnw test
```

Os testes de integração usam Testcontainers com PostgreSQL 17, aplicam Flyway e validam o esquema com Hibernate. Não utilizam o banco local.

Cobertura: cinco cenários de aceite, campos ausentes/nulos, descrição vazia/em branco, JSON malformado, histórico inicial, ausência de gravações nas rejeições, protocolos simultâneos e esgotamento da sequência. Testes de domínio verificam as regras sem depender do Service.
