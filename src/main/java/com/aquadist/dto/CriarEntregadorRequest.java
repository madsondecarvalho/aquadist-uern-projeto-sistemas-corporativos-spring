package com.aquadist.dto;

import com.aquadist.model.Zona;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CriarEntregadorRequest(
        @NotBlank(message = "nome e obrigatorio") @Size(max = 120) String nome,
        @NotBlank(message = "email e obrigatorio") @Email(message = "email invalido") @Size(max = 180) String email,
        @NotBlank(message = "senha e obrigatoria") @Size(min = 6, max = 60, message = "senha deve ter entre 6 e 60 caracteres") String senha,
        @Size(max = 30) String telefone,
        @NotNull(message = "zona e obrigatoria") Zona zona,
        @Min(value = 1, message = "capacidade de carga deve ser maior que zero") int capacidadeCarga,
        @Size(max = 80) String veiculoModelo,
        @Size(max = 10) String veiculoPlaca
) {
}
