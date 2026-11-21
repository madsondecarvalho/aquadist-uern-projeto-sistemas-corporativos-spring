package com.aquadist.event;

import com.aquadist.service.AtribuicaoEntregadorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PedidoCriadoListener {

    private static final Logger log = LoggerFactory.getLogger(PedidoCriadoListener.class);

    private final AtribuicaoEntregadorService atribuicaoService;

    public PedidoCriadoListener(AtribuicaoEntregadorService atribuicaoService) {
        this.atribuicaoService = atribuicaoService;
    }

    @EventListener
    public void aoCriarPedido(PedidoCriadoEvent event) {
        try {
            atribuicaoService.tentarAtribuir(event.pedidoId());
        } catch (RuntimeException ex) {
            // A atribuicao nao deve impedir a criacao do pedido; o admin pode atribuir manualmente.
            log.error("Falha ao atribuir entregador ao pedido {}", event.pedidoId(), ex);
        }
    }
}
