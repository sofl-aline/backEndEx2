CREATE SEQUENCE protocolo_os_seq START WITH 1 INCREMENT BY 1 MAXVALUE 1679615 NO CYCLE;

CREATE TABLE tecnicos
(
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    telefone VARCHAR(20),
    especialidade VARCHAR(100),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE ordens_servico
(
    id BIGSERIAL PRIMARY KEY,
    numero_protocolo VARCHAR(30) NOT NULL UNIQUE,
    cliente_id BIGINT NOT NULL REFERENCES clientes (id),
    equipamento_id BIGINT NOT NULL REFERENCES equipamentos (id),
    tecnico_id BIGINT REFERENCES tecnicos (id),
    status VARCHAR(30) NOT NULL,
    descricao_problema TEXT NOT NULL,
    data_abertura TIMESTAMP NOT NULL,
    data_finalizacao TIMESTAMP,
    data_entrega TIMESTAMP
);

CREATE TABLE historico_status
(
    id BIGSERIAL PRIMARY KEY,
    ordem_servico_id BIGINT NOT NULL REFERENCES ordens_servico (id),
    status_anterior VARCHAR(30),
    novo_status VARCHAR(30) NOT NULL,
    data_alteracao TIMESTAMP NOT NULL,
    responsavel VARCHAR(150) NOT NULL,
    observacao VARCHAR(500)
);
