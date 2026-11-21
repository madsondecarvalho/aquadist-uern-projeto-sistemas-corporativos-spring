package com.aquadist.dto;

import jakarta.validation.constraints.Size;

public record CancelarPedidoRequest(
        @Size(max = 500, message = "motivo deve ter no maximo 500 caracteres") String motivo
) {
}
