package com.aquadist.controller;

import com.aquadist.dto.ClienteResponse;
import com.aquadist.dto.VasilhamesClienteResponse;
import com.aquadist.service.ClienteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clientes")
@PreAuthorize("hasRole('CLIENTE')")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping("/me")
    public ClienteResponse me(Authentication authentication) {
        return clienteService.me(authentication.getName());
    }

    @GetMapping("/me/vasilhames")
    public VasilhamesClienteResponse vasilhames(Authentication authentication) {
        return clienteService.vasilhames(authentication.getName());
    }
}
