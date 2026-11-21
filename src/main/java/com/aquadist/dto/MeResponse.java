package com.aquadist.dto;

import com.aquadist.model.PerfilUsuario;

public record MeResponse(
        String nome,
        String email,
        PerfilUsuario perfil
) {
}
