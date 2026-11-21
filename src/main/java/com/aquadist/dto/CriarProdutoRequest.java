package com.aquadist.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CriarProdutoRequest(
        @NotBlank(message = "codigo e obrigatorio") @Size(max = 20) String codigo,
        @NotBlank(message = "nome e obrigatorio") @Size(max = 100) String nome,
        @Min(value = 1, message = "volume deve ser maior que zero") int volumeLitros,
        @NotNull(message = "preco unitario e obrigatorio")
        @DecimalMin(value = "0.01", message = "preco unitario deve ser maior que zero") BigDecimal precoUnitario,
        @NotNull(message = "taxa de caucao e obrigatoria")
        @DecimalMin(value = "0.00", message = "taxa de caucao nao pode ser negativa") BigDecimal taxaCaucao
) {
}
