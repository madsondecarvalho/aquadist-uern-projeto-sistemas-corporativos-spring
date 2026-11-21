package com.aquadist.dto;

import com.aquadist.model.Endereco;
import com.aquadist.model.Zona;

public record EnderecoResponse(
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String cep,
        Zona zona
) {
    public static EnderecoResponse from(Endereco endereco) {
        if (endereco == null) {
            return null;
        }
        return new EnderecoResponse(
                endereco.getLogradouro(),
                endereco.getNumero(),
                endereco.getComplemento(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getUf(),
                endereco.getCep(),
                endereco.getZona()
        );
    }
}
