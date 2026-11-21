package com.aquadist.dto;

import com.aquadist.model.TipoCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CriarClienteRequest(
        @NotBlank(message = "nome e obrigatorio") @Size(max = 120) String nome,
        @NotBlank(message = "email e obrigatorio") @Email(message = "email invalido") @Size(max = 180) String email,
        @NotBlank(message = "senha e obrigatoria") @Size(min = 6, max = 60, message = "senha deve ter entre 6 e 60 caracteres") String senha,
        @NotNull(message = "tipo e obrigatorio") TipoCliente tipo,
        @NotBlank(message = "documento e obrigatorio") @Size(max = 20) String documento,
        @Size(max = 30) String telefone,
        @Valid @NotNull(message = "endereco e obrigatorio") EnderecoRequest endereco,
        @DecimalMin(value = "0.00", message = "limite de credito nao pode ser negativo") BigDecimal limiteCredito
) {
}
