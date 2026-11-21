package com.aquadist.controller;

import com.aquadist.dto.ConcluirEntregaRequest;
import com.aquadist.dto.EntregadorResponse;
import com.aquadist.dto.EstoqueEntregadorResponse;
import com.aquadist.dto.PedidoResponse;
import com.aquadist.dto.VasilhameResponse;
import com.aquadist.service.EntregadorService;
import com.aquadist.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entregador")
@PreAuthorize("hasRole('ENTREGADOR')")
public class EntregadorController {

    private final EntregadorService entregadorService;
    private final PedidoService pedidoService;

    public EntregadorController(EntregadorService entregadorService, PedidoService pedidoService) {
        this.entregadorService = entregadorService;
        this.pedidoService = pedidoService;
    }

    @GetMapping("/me")
    public EntregadorResponse me(Authentication authentication) {
        return entregadorService.me(authentication.getName());
    }

    @GetMapping("/pedidos")
    public List<PedidoResponse> pedidos(Authentication authentication) {
        return entregadorService.pedidos(authentication.getName());
    }

    @GetMapping("/pedidos/{id}")
    public PedidoResponse pedido(@PathVariable Long id, Authentication authentication) {
        return pedidoService.buscar(id, authentication.getName());
    }

    @GetMapping("/pedidos/{id}/vasilhames-cliente")
    public List<VasilhameResponse> vasilhamesCliente(@PathVariable Long id, Authentication authentication) {
        return entregadorService.vasilhamesClienteDoPedido(id, authentication.getName());
    }

    @PostMapping("/pedidos/{id}/iniciar-transito")
    public PedidoResponse iniciarTransito(@PathVariable Long id, Authentication authentication) {
        return pedidoService.iniciarTransito(id, authentication.getName());
    }

    @PostMapping("/pedidos/{id}/concluir")
    public PedidoResponse concluir(@PathVariable Long id,
                                   @RequestBody @Valid ConcluirEntregaRequest request,
                                   Authentication authentication) {
        return pedidoService.concluirEntrega(id, authentication.getName(), request);
    }

    @GetMapping("/estoque")
    public EstoqueEntregadorResponse estoque(Authentication authentication) {
        return entregadorService.estoque(authentication.getName());
    }
}
