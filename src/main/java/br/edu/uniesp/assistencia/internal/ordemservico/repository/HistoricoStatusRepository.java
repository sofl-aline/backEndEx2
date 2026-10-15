package br.edu.uniesp.assistencia.internal.ordemservico.repository;

import br.edu.uniesp.assistencia.internal.ordemservico.entity.HistoricoStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatusEntity, Long> {
}
