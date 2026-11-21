package com.aquadist.dto;

import com.aquadist.model.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarPedidoRequest(
        @NotEmpty(message = "o pedido deve ter ao menos um item")
        List<@Valid ItemPedidoRequest> itens,
        @NotNull(message = "forma de pagamento e obrigatoria") FormaPagamento formaPagamento,
        @Size(max = 500, message = "observacao deve ter no maximo 500 caracteres") String observacao
) {
}
