package com.aquadist.repository;

import com.aquadist.model.Pedido;
import com.aquadist.model.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByCriadoEmDesc(Long clienteId);

    List<Pedido> findByEntregadorIdAndStatusInOrderByCriadoEmAsc(
            Long entregadorId, Collection<StatusPedido> status);

    List<Pedido> findAllByOrderByCriadoEmDesc();

    List<Pedido> findByStatusOrderByCriadoEmDesc(StatusPedido status);

    long countByStatus(StatusPedido status);

    @Query("select sum(p.valorTotal) from Pedido p where p.cliente.id = :clienteId and p.status in :status")
    BigDecimal somarValorPorStatus(@Param("clienteId") Long clienteId,
                                   @Param("status") Collection<StatusPedido> status);

    @Query("""
            select coalesce(sum(i.quantidade), 0)
              from ItemPedido i
             where i.pedido.entregador.id = :entregadorId
               and i.pedido.status in :status
            """)
    Long somarGarrafoesPorEntregador(@Param("entregadorId") Long entregadorId,
                                     @Param("status") Collection<StatusPedido> status);
}
