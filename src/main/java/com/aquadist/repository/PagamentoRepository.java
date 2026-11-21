package com.aquadist.repository;

import com.aquadist.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    List<Pagamento> findByPedidoId(Long pedidoId);
}
