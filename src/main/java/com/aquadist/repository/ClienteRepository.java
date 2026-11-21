package com.aquadist.repository;

import com.aquadist.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByUsuarioEmailIgnoreCase(String email);

    boolean existsByDocumento(String documento);
}
