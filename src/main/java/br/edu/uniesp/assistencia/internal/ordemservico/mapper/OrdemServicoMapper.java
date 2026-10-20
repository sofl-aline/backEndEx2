package br.edu.uniesp.assistencia.internal.ordemservico.mapper;

import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoResponse;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.OrdemServicoEntity;

public final class OrdemServicoMapper {

    private OrdemServicoMapper() {
    }

    public static CriarOrdemServicoResponse converterParaResposta(OrdemServicoEntity ordem) {
        return new CriarOrdemServicoResponse(ordem.getId(), ordem.getNumeroProtocolo(),
                ordem.getStatus(), ordem.getDataAbertura());
    }
}
