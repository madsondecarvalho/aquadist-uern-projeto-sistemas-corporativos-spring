package com.aquadist.dto;

import com.aquadist.model.Cliente;
import com.aquadist.model.TipoCliente;
import com.aquadist.model.Zona;

import java.math.BigDecimal;

public record ClienteResponse(
        Long id,
        String nome,
        String email,
        TipoCliente tipo,
        String documento,
        String telefone,
        EnderecoResponse endereco,
        Zona zona,
        BigDecimal limiteCredito,
        BigDecimal saldoDevedor,
        BigDecimal limiteDisponivel,
        boolean ativo
) {
    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getUsuario().getNome(),
                cliente.getUsuario().getEmail(),
                cliente.getTipo(),
                cliente.getDocumento(),
                cliente.getTelefone(),
                EnderecoResponse.from(cliente.getEndereco()),
                cliente.getEndereco() == null ? null : cliente.getEndereco().getZona(),
                cliente.getLimiteCredito(),
                cliente.getSaldoDevedor(),
                cliente.getLimiteDisponivel(),
                cliente.isAtivo()
        );
    }
}
