package com.aquadist.dto;

import jakarta.validation.constraints.NotNull;

public record AtribuirEntregadorRequest(
        @NotNull(message = "entregadorId e obrigatorio") Long entregadorId
) {
}
