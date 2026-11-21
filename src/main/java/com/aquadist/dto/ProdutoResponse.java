package com.aquadist.dto;

import com.aquadist.model.Produto;

import java.math.BigDecimal;

public record ProdutoResponse(
        Long id,
        String codigo,
        String nome,
        int volumeLitros,
        BigDecimal precoUnitario,
        BigDecimal taxaCaucao,
        long estoqueCentral,
        long vasilhamesVencidos,
        boolean ativo
) {
    public static ProdutoResponse from(Produto produto, long estoqueCentral, long vencidos) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getCodigo(),
                produto.getNome(),
                produto.getVolumeLitros(),
                produto.getPrecoUnitario(),
                produto.getTaxaCaucao(),
                estoqueCentral,
                vencidos,
                produto.isAtivo()
        );
    }
}
