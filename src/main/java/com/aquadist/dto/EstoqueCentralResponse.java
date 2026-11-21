package com.aquadist.dto;

import java.util.List;

public record EstoqueCentralResponse(
        List<Item> itens,
        long totalDisponiveis,
        long totalEmTransito,
        long totalComClientes,
        long totalVencidos,
        long totalDescartados
) {
    public record Item(
            Long produtoId,
            String codigo,
            String nome,
            int volumeLitros,
            long disponiveisNaCentral,
            long emTransito,
            long comClientes,
            long vencidos,
            long descartados
    ) {
    }
}
