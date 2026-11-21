package com.aquadist.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(
        @NotNull(message = "produtoId e obrigatorio") Long produtoId,
        @Min(value = 1, message = "quantidade deve ser maior que zero") int quantidade,
        @Min(value = 0, message = "vasilhamesDevolvidos nao pode ser negativo") int vasilhamesDevolvidos
) {
}
