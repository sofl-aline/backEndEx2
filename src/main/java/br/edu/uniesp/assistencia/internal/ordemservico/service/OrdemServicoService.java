package br.edu.uniesp.assistencia.internal.ordemservico.service;

import br.edu.uniesp.assistencia.internal.ordemservico.repository.OrdemServicoRepository;
import br.edu.uniesp.assistencia.internal.cliente.repository.ClienteRepository;
import br.edu.uniesp.assistencia.internal.equipamento.repository.EquipamentoRepository;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoRequest;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoResponse;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.OrdemServicoEntity;
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
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;

    @Transactional
    public CriarOrdemServicoResponse abrirOrdemServico(CriarOrdemServicoRequest request) {
        var cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", request.clienteId()));
        var equipamento = equipamentoRepository.findById(request.equipamentoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento", request.equipamentoId()));

        if (!cliente.isAtivo()) {
            throw new RegraNegocioException(
                    "Não é permitido abrir Ordem de Serviço para um cliente inativo.");
        }

        if (!equipamento.getCliente().getId().equals(cliente.getId())) {
            throw new RegraNegocioException("O equipamento não pertence ao cliente informado.");
        }

        String dataProtocolo = LocalDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Long sequencial = ordemServicoRepository.proximoNumeroProtocolo();
        String protocolo = String.format(Locale.ROOT, "OS-%s-%04d", dataProtocolo, sequencial);

        var ordem = ordemServicoRepository.save(new OrdemServicoEntity(
                protocolo, cliente, equipamento, request.descricaoProblema()));
        return new CriarOrdemServicoResponse(ordem.getId(), ordem.getNumeroProtocolo(),
                ordem.getStatus(), ordem.getDataAbertura());
    }
}
