package br.edu.uniesp.assistencia.internal.ordemservico.entity;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdemServicoEntityTest {

    private ClienteEntity cliente;
    private EquipamentoEntity equipamento;

    @BeforeEach
    void preparar() {
        cliente = new ClienteEntity("Maria", "12345678901", "maria@example.com");
        ReflectionTestUtils.setField(cliente, "id", 1L);
        equipamento = new EquipamentoEntity("Notebook", "Dell", "Inspiron", "ABC");
        equipamento.associarCliente(cliente);
    }

    @Test
    void criarRecebidaComHistoricoInicial() {
        var antes = LocalDateTime.now();
        var ordem = new OrdemServicoEntity("OS-20261020-0001", cliente, equipamento, "Não liga");
        assertThat(ordem.getStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
        assertThat(ordem.getDataAbertura()).isBetween(antes, LocalDateTime.now());
        assertThat(ordem.getHistorico()).singleElement().satisfies(entrada -> {
            assertThat(entrada.getOrdemServico()).isSameAs(ordem);
            assertThat(entrada.getStatusAnterior()).isNull();
            assertThat(entrada.getNovoStatus()).isEqualTo(StatusOrdemServico.RECEBIDA);
            assertThat(entrada.getResponsavel()).isEqualTo("SISTEMA");
            assertThat(entrada.getObservacao()).isEqualTo("Abertura da ordem de serviço.");
        });
    }

    @Test
    void impedirAberturaParaClienteInativo() {
        cliente.inativar();
        assertThatThrownBy(() -> new OrdemServicoEntity("OS-20261020-0001", cliente, equipamento, "Não liga"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não é permitido abrir Ordem de Serviço para um cliente inativo.");
    }

    @Test
    void impedirAberturaComEquipamentoDeOutroCliente() {
        var outro = new ClienteEntity("João", "98765432100", "joao@example.com");
        ReflectionTestUtils.setField(outro, "id", 2L);
        equipamento.associarCliente(outro);
        assertThatThrownBy(() -> new OrdemServicoEntity("OS-20261020-0001", cliente, equipamento, "Não liga"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O equipamento informado não pertence ao cliente indicado.");
    }

    @Test
    void compararProprietarioPorId() {
        var mesmaPessoa = new ClienteEntity("Maria", "12345678901", "maria@example.com");
        ReflectionTestUtils.setField(mesmaPessoa, "id", 1L);
        equipamento.associarCliente(mesmaPessoa);
        var ordem = new OrdemServicoEntity("OS-20261020-0001", cliente, equipamento, "Não liga");
        assertThat(ordem.getCliente()).isSameAs(cliente);
    }
}
