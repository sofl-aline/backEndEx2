package br.edu.uniesp.assistencia;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.cliente.repository.ClienteRepository;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.internal.equipamento.repository.EquipamentoRepository;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoRequest;
import br.edu.uniesp.assistencia.internal.ordemservico.service.OrdemServicoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AssistenciaUniespApplicationTests {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @DynamicPropertySource
    static void configurarBanco(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ClienteRepository clientes;
    @Autowired EquipamentoRepository equipamentos;
    @Autowired OrdemServicoService service;

    private ClienteEntity cliente;
    private EquipamentoEntity equipamento;

    @BeforeEach
    void preparar() {
        jdbc.execute("TRUNCATE clientes, equipamentos, tecnicos, ordens_servico, historico_status, "
                + "diagnosticos, orcamentos, itens_orcamento, pecas, os_pecas, anexos, usuarios RESTART IDENTITY");
        jdbc.execute("ALTER SEQUENCE protocolo_os_seq RESTART WITH 1");
        cliente = clientes.save(new ClienteEntity("Maria", "12345678901", "maria@example.com"));
        equipamento = new EquipamentoEntity("Notebook", "Dell", "Inspiron", "ABC");
        equipamento.associarCliente(cliente);
        equipamento = equipamentos.save(equipamento);
    }

    @Test
    void abrirOrdemComHistorico() throws Exception {
        var antes = LocalDateTime.now();
        mvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content(payload(cliente.getId(), equipamento.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/ordens/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numeroProtocolo", matchesPattern("OS-[0-9]{8}-[0-9A-Z]{4}")))
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.dataAbertura").exists());
        var ordem = jdbc.queryForMap("SELECT * FROM ordens_servico");
        var abertura = ((java.sql.Timestamp) ordem.get("data_abertura")).toLocalDateTime();
        assertThat(abertura).isBetween(antes.minusNanos(1000), LocalDateTime.now());
        assertThat(ordem.get("numero_protocolo")).isEqualTo("OS-" + abertura.format(DateTimeFormatter.BASIC_ISO_DATE) + "-0001");
        var historico = jdbc.queryForMap("SELECT * FROM historico_status");
        assertThat(historico.get("ordem_servico_id")).isEqualTo(ordem.get("id"));
        assertThat(historico.get("status_anterior")).isNull();
        assertThat(historico.get("novo_status")).isEqualTo("RECEBIDA");
        assertThat(historico.get("responsavel")).isEqualTo("SISTEMA");
        assertThat(historico.get("observacao")).isEqualTo("Abertura da ordem de serviço.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"clienteId\":null,\"equipamentoId\":null,\"descricaoProblema\":null}"})
    void rejeitarCamposAusentesOuNulos(String corpo) throws Exception {
        mvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.erros.clienteId").exists())
                .andExpect(jsonPath("$.erros.equipamentoId").exists())
                .andExpect(jsonPath("$.erros.descricaoProblema").exists());
        conferirSemGravacoes();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejeitarDescricaoEmBranco(String descricao) throws Exception {
        mvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":1,\"equipamentoId\":1,\"descricaoProblema\":\"" + descricao + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.erros.descricaoProblema").exists());
        conferirSemGravacoes();
    }

    @Test
    void rejeitarJsonInvalido() throws Exception {
        mvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        conferirSemGravacoes();
    }

    @Test
    void rejeitarClienteInexistente() throws Exception {
        conferirErro(999L, equipamento.getId(), 404, "Cliente não encontrado para o identificador: 999");
    }

    @Test
    void rejeitarEquipamentoInexistente() throws Exception {
        conferirErro(cliente.getId(), 999L, 404, "Equipamento não encontrado para o identificador: 999");
    }

    @Test
    void rejeitarClienteInativo() throws Exception {
        cliente.inativar();
        clientes.save(cliente);
        conferirErro(cliente.getId(), equipamento.getId(), 422,
                "Não é permitido abrir Ordem de Serviço para um cliente inativo.");
    }

    @Test
    void rejeitarEquipamentoDeOutroCliente() throws Exception {
        var outro = clientes.save(new ClienteEntity("João", "98765432100", "joao@example.com"));
        conferirErro(outro.getId(), equipamento.getId(), 422,
                "O equipamento informado não pertence ao cliente indicado.");
    }

    @Test
    void gerarProtocolosDistintosEmAberturasSimultaneas() throws Exception {
        var request = new CriarOrdemServicoRequest(cliente.getId(), equipamento.getId(), "Não liga");
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tarefas = IntStream.range(0, 12)
                    .mapToObj(i -> (Callable<String>) () -> service.abrirOrdemServico(request).numeroProtocolo()).toList();
            var resultados = executor.invokeAll(tarefas);
            var protocolos = new java.util.HashSet<String>();
            for (var resultado : resultados) {
                assertThat(protocolos.add(resultado.get())).isTrue();
            }
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ordens_servico", Long.class)).isEqualTo(12);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_status", Long.class)).isEqualTo(12);
    }

    @Test
    void respeitarLimiteDoProtocoloSemReutilizarNumeros() throws Exception {
        jdbc.execute("ALTER SEQUENCE protocolo_os_seq RESTART WITH 1679615");
        mvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content(payload(cliente.getId(), equipamento.getId())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.numeroProtocolo", matchesPattern("OS-[0-9]{8}-ZZZZ")));
        mvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content(payload(cliente.getId(), equipamento.getId())))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail").value("A capacidade de geração de protocolos foi esgotada."));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ordens_servico", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_status", Long.class)).isEqualTo(1);
    }

    private void conferirErro(Long clienteId, Long equipamentoId, int status, String mensagem) throws Exception {
        mvc.perform(post("/ordens").contentType(MediaType.APPLICATION_JSON).content(payload(clienteId, equipamentoId)))
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.detail").value(mensagem))
                .andExpect(jsonPath("$.instance").value("/ordens"));
        conferirSemGravacoes();
    }

    private void conferirSemGravacoes() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ordens_servico", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_status", Long.class)).isZero();
    }

    private String payload(Long clienteId, Long equipamentoId) {
        return "{\"clienteId\":%d,\"equipamentoId\":%d,\"descricaoProblema\":\"Notebook não liga\"}"
                .formatted(clienteId, equipamentoId);
    }
}
