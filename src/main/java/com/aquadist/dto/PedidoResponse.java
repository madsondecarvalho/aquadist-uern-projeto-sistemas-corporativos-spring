package com.aquadist.dto;

import com.aquadist.model.FormaPagamento;
import com.aquadist.model.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long id,
        String status,
        Long clienteId,
        String clienteNome,
        String clienteTelefone,
        Long entregadorId,
        String entregadorNome,
        FormaPagamento formaPagamento,
        boolean pagamentoConfirmado,
        LocalDateTime pagoEm,
        EnderecoResponse enderecoEntrega,
        List<ItemPedidoResponse> itens,
        int totalGarrafoes,
        BigDecimal valorProdutos,
        BigDecimal valorCaucao,
        BigDecimal valorTotal,
        String observacao,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm,
        LocalDateTime despachadoEm,
        LocalDateTime entregueEm,
        LocalDateTime canceladoEm,
        String motivoCancelamento
) {
    public static PedidoResponse from(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getStatus().name(),
                pedido.getCliente().getId(),
                pedido.getCliente().getUsuario().getNome(),
                pedido.getCliente().getTelefone(),
                pedido.getEntregador() == null ? null : pedido.getEntregador().getId(),
                pedido.getEntregador() == null ? null : pedido.getEntregador().getUsuario().getNome(),
                pedido.getFormaPagamento(),
                pedido.isPagamentoConfirmado(),
                pedido.getPagoEm(),
                EnderecoResponse.from(pedido.getEnderecoEntrega()),
                pedido.getItens().stream().map(ItemPedidoResponse::from).toList(),
                pedido.totalGarrafoes(),
                pedido.getValorProdutos(),
                pedido.getValorCaucao(),
                pedido.getValorTotal(),
                pedido.getObservacao(),
                pedido.getCriadoEm(),
                pedido.getAtualizadoEm(),
                pedido.getDespachadoEm(),
                pedido.getEntregueEm(),
                pedido.getCanceladoEm(),
                pedido.getMotivoCancelamento()
        );
    }
}
