package com.aquadist.dto;

import com.aquadist.model.Zona;

import java.util.List;

public record EstoqueEntregadorResponse(
        Long entregadorId,
        String nome,
        Zona zona,
        int capacidadeCarga,
        long cargaAtual,
        int capacidadeDisponivel,
        List<Item> itens,
        List<VasilhameResponse> vasilhames
) {
    public record Item(
            Long produtoId,
            String produtoNome,
            long quantidade
    ) {
    }
}
