package com.aquadist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SimulacaoRequest(
        @NotEmpty(message = "informe ao menos um item")
        List<@Valid ItemPedidoRequest> itens
) {
}
