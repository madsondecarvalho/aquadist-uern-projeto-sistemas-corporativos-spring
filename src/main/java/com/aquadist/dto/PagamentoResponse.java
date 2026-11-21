package com.aquadist.dto;

import com.aquadist.model.Pagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponse(
        Long id,
        BigDecimal valor,
        Long pedidoId,
        String descricao,
        LocalDateTime criadoEm
) {
    public static PagamentoResponse from(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getValor(),
                pagamento.getPedido() == null ? null : pagamento.getPedido().getId(),
                pagamento.getDescricao(),
                pagamento.getCriadoEm()
        );
    }
}
