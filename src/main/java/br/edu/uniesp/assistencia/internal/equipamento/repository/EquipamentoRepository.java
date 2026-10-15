package br.edu.uniesp.assistencia.internal.equipamento.repository;

import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipamentoRepository extends JpaRepository<EquipamentoEntity, Long> {
}
