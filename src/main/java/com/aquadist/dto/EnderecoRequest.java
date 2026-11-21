package com.aquadist.dto;

import com.aquadist.model.Endereco;
import com.aquadist.model.Zona;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank(message = "logradouro e obrigatorio") @Size(max = 150) String logradouro,
        @NotBlank(message = "numero e obrigatorio") @Size(max = 20) String numero,
        @Size(max = 80) String complemento,
        @NotBlank(message = "bairro e obrigatorio") @Size(max = 100) String bairro,
        @NotBlank(message = "cidade e obrigatoria") @Size(max = 100) String cidade,
        @NotBlank(message = "uf e obrigatoria") @Size(max = 2) String uf,
        @NotBlank(message = "cep e obrigatorio") @Size(max = 10) String cep,
        @NotNull(message = "zona e obrigatoria") Zona zona
) {
    public Endereco toEmbeddable() {
        return new Endereco(logradouro, numero, complemento, bairro, cidade, uf, cep, zona);
    }
}
