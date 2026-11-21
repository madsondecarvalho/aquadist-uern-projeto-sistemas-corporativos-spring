package com.aquadist.dto;

import java.util.List;

public record VasilhamesClienteResponse(
        Long clienteId,
        String clienteNome,
        int totalValidos,
        int totalVencidos,
        List<Resumo> resumo,
        List<VasilhameResponse> vasilhames
) {
    public record Resumo(
            Long produtoId,
            String produtoNome,
            long validos,
            long vencidos
    ) {
    }
}
