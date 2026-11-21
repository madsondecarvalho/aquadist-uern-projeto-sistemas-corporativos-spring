package com.aquadist.dto;

import java.math.BigDecimal;
import java.util.List;

public record SimulacaoResponse(
        BigDecimal valorProdutos,
        BigDecimal valorCaucao,
        BigDecimal valorTotal,
        boolean limiteSuficiente,
        BigDecimal limiteCredito,
        BigDecimal saldoDevedor,
        BigDecimal valorPedidosEmAberto,
        BigDecimal limiteDisponivel,
        List<ItemSimulacao> itens,
        List<String> avisos
) {
    public record ItemSimulacao(
            Long produtoId,
            String produtoNome,
            int quantidade,
            int vasilhamesDevolvidos,
            long vasilhamesDisponiveisTroca,
            int quantidadeCaucao,
            BigDecimal valorCaucao,
            BigDecimal subtotal
    ) {
    }
}
