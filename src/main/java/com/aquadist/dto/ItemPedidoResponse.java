package com.aquadist.dto;

import com.aquadist.model.ItemPedido;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long produtoId,
        String produtoCodigo,
        String produtoNome,
        int volumeLitros,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal taxaCaucaoAplicada,
        int vasilhamesDevolvidos,
        int vasilhamesValidosRecebidos,
        int vasilhamesRecusadosVencidos,
        int quantidadeCaucao,
        BigDecimal valorCaucao,
        BigDecimal subtotal
) {
    public static ItemPedidoResponse from(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getProduto().getId(),
                item.getProduto().getCodigo(),
                item.getProduto().getNome(),
                item.getProduto().getVolumeLitros(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getTaxaCaucaoAplicada(),
                item.getVasilhamesDevolvidos(),
                item.getVasilhamesValidosRecebidos(),
                item.getVasilhamesRecusadosVencidos(),
                item.getQuantidadeCaucao(),
                item.getValorCaucao(),
                item.getSubtotal()
        );
    }
}
