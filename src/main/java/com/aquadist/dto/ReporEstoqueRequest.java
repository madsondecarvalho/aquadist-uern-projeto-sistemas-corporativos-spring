package com.aquadist.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ReporEstoqueRequest(
        @NotNull(message = "produtoId e obrigatorio") Long produtoId,
        @Min(value = 1, message = "quantidade deve ser maior que zero") int quantidade,
        @NotNull(message = "dataValidade e obrigatoria")
        @FutureOrPresent(message = "data de validade deve ser hoje ou futura") LocalDate dataValidade
) {
}
