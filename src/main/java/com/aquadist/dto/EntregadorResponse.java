package com.aquadist.dto;

import com.aquadist.model.Entregador;
import com.aquadist.model.Zona;

public record EntregadorResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        Zona zona,
        int capacidadeCarga,
        long cargaAtual,
        int capacidadeDisponivel,
        String veiculoModelo,
        String veiculoPlaca,
        boolean ativo
) {
    public static EntregadorResponse from(Entregador entregador, long cargaAtual) {
        return new EntregadorResponse(
                entregador.getId(),
                entregador.getUsuario().getNome(),
                entregador.getUsuario().getEmail(),
                entregador.getTelefone(),
                entregador.getZona(),
                entregador.getCapacidadeCarga(),
                cargaAtual,
                Math.max(0, entregador.getCapacidadeCarga() - (int) cargaAtual),
                entregador.getVeiculoModelo(),
                entregador.getVeiculoPlaca(),
                entregador.isAtivo()
        );
    }
}
