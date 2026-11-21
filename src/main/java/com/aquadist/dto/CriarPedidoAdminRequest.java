package com.aquadist.dto;

import com.aquadist.model.FormaPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Pedido criado pelo administrador em nome de um cliente.
 * O administrador precisa confirmar o recebimento do pagamento no ato da criação.
 */
public record CriarPedidoAdminRequest(
        @NotNull(message = "clienteId e obrigatorio") Long clienteId,
        @NotEmpty(message = "o pedido deve ter ao menos um item")
        List<@Valid ItemPedidoRequest> itens,
        @NotNull(message = "forma de pagamento e obrigatoria") FormaPagamento formaPagamento,
        @Size(max = 500, message = "observacao deve ter no maximo 500 caracteres") String observacao,
        @AssertTrue(message = "o administrador deve confirmar o recebimento do pagamento")
        boolean pagamentoConfirmado
) {
}
