package com.aquadist.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PagamentoRequest(
        @NotNull(message = "valor e obrigatorio")
        @DecimalMin(value = "0.01", message = "valor deve ser maior que zero") BigDecimal valor,
        @Size(max = 200) String descricao
) {
}
