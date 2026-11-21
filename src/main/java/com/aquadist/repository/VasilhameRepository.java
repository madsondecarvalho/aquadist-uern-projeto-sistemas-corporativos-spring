package com.aquadist.repository;

import com.aquadist.model.StatusVasilhame;
import com.aquadist.model.Vasilhame;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VasilhameRepository extends JpaRepository<Vasilhame, Long> {

    Optional<Vasilhame> findByCodigoIgnoreCase(String codigo);

    List<Vasilhame> findByCodigoIn(Collection<String> codigos);

    List<Vasilhame> findByClienteIdAndStatusOrderByDataValidadeAsc(Long clienteId, StatusVasilhame status);

    long countByClienteIdAndProdutoIdAndStatusAndDataValidadeGreaterThanEqual(
            Long clienteId, Long produtoId, StatusVasilhame status, LocalDate data);

    long countByClienteIdAndProdutoIdAndStatusAndDataValidadeLessThan(
            Long clienteId, Long produtoId, StatusVasilhame status, LocalDate data);

    List<Vasilhame> findByProdutoIdAndStatusAndDataValidadeGreaterThanEqualOrderByDataValidadeAsc(
            Long produtoId, StatusVasilhame status, LocalDate data, Pageable pageable);

    List<Vasilhame> findByPedidoIdAndStatus(Long pedidoId, StatusVasilhame status);

    List<Vasilhame> findByPedidoIdAndStatusAndProdutoId(Long pedidoId, StatusVasilhame status, Long produtoId);

    List<Vasilhame> findByEntregadorIdAndStatus(Long entregadorId, StatusVasilhame status);

    long countByProdutoIdAndStatusAndDataValidadeGreaterThanEqual(
            Long produtoId, StatusVasilhame status, LocalDate data);

    long countByProdutoIdAndStatusAndDataValidadeLessThan(
            Long produtoId, StatusVasilhame status, LocalDate data);

    long countByProdutoIdAndStatus(Long produtoId, StatusVasilhame status);

    long countByStatus(StatusVasilhame status);
}
