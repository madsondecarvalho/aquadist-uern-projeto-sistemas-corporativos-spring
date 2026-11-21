package com.aquadist.dto;

import com.aquadist.model.FormaPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConcluirEntregaRequest(
        @NotNull(message = "a lista de vasilhames devolvidos e obrigatoria; use [] quando nao houver")
        @Size(max = 1000) List<String> codigosVasilhamesDevolvidos,
        FormaPagamento formaPagamentoRecebida,
        @Size(max = 500, message = "observacao deve ter no maximo 500 caracteres") String observacao
) {
}
