package com.aquadist.controller;

import com.aquadist.dto.CancelarPedidoRequest;
import com.aquadist.dto.CriarPedidoRequest;
import com.aquadist.dto.PedidoResponse;
import com.aquadist.dto.SimulacaoRequest;
import com.aquadist.dto.SimulacaoResponse;
import com.aquadist.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CLIENTE')")
    public PedidoResponse criar(@RequestBody @Valid CriarPedidoRequest request,
                                Authentication authentication) {
        return pedidoService.criar(request, authentication.getName());
    }

    @PostMapping("/simulacao")
    @PreAuthorize("hasRole('CLIENTE')")
    public SimulacaoResponse simular(@RequestBody @Valid SimulacaoRequest request,
                                     Authentication authentication) {
        return pedidoService.simular(request, authentication.getName());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    public List<PedidoResponse> meusPedidos(Authentication authentication) {
        return pedidoService.listarDoCliente(authentication.getName());
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Long id, Authentication authentication) {
        return pedidoService.buscar(id, authentication.getName());
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('CLIENTE')")
    public PedidoResponse cancelar(@PathVariable Long id,
                                   @RequestBody(required = false) @Valid CancelarPedidoRequest request,
                                   Authentication authentication) {
        return pedidoService.cancelar(id, authentication.getName(), request);
    }
}
