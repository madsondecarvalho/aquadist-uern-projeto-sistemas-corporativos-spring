package com.aquadist.service;

import com.aquadist.dto.MeResponse;
import com.aquadist.exception.RecursoNaoEncontradoException;
import com.aquadist.model.Usuario;
import com.aquadist.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public MeResponse me(String email) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado."));
        return new MeResponse(usuario.getNome(), usuario.getEmail(), usuario.getPerfil());
    }
}
