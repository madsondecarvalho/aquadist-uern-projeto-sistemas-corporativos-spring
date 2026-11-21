package com.aquadist.dto;

import com.aquadist.model.StatusVasilhame;
import com.aquadist.model.Vasilhame;

import java.time.LocalDate;

public record VasilhameResponse(
        Long id,
        String codigo,
        Long produtoId,
        String produtoNome,
        int volumeLitros,
        LocalDate dataValidade,
        StatusVasilhame status,
        boolean vencido
) {
    public static VasilhameResponse from(Vasilhame vasilhame, LocalDate referencia) {
        return new VasilhameResponse(
                vasilhame.getId(),
                vasilhame.getCodigo(),
                vasilhame.getProduto().getId(),
                vasilhame.getProduto().getNome(),
                vasilhame.getProduto().getVolumeLitros(),
                vasilhame.getDataValidade(),
                vasilhame.getStatus(),
                vasilhame.estaVencido(referencia)
        );
    }
}
