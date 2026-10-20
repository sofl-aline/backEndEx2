package br.edu.uniesp.assistencia.internal.ordemservico.service;

import br.edu.uniesp.assistencia.internal.ordemservico.repository.OrdemServicoRepository;
import br.edu.uniesp.assistencia.internal.cliente.repository.ClienteRepository;
import br.edu.uniesp.assistencia.internal.equipamento.repository.EquipamentoRepository;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoRequest;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoResponse;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.OrdemServicoEntity;
import br.edu.uniesp.assistencia.internal.ordemservico.mapper.OrdemServicoMapper;
import br.edu.uniesp.assistencia.shared.exception.RecursoNaoEncontradoException;
import br.edu.uniesp.assistencia.shared.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
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

        LocalDateTime dataAbertura = LocalDateTime.now();
        Long sequencial;
        try {
            sequencial = ordemServicoRepository.proximoNumeroProtocolo();
        } catch (DataAccessException exception) {
            if (exception.getMostSpecificCause() instanceof SQLException causa
                    && "2200H".equals(causa.getSQLState())) {
                throw new RegraNegocioException("A capacidade de geração de protocolos foi esgotada.");
            }
            throw exception;
        }
        String sufixo = Long.toString(sequencial, 36).toUpperCase(Locale.ROOT);
        String protocolo = "OS-" + dataAbertura.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + "0".repeat(4 - sufixo.length()) + sufixo;

        var ordem = ordemServicoRepository.save(new OrdemServicoEntity(
                protocolo, cliente, equipamento, request.descricaoProblema(), dataAbertura));
        return OrdemServicoMapper.converterParaResposta(ordem);
    }
}
