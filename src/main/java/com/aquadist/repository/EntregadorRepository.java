package com.aquadist.repository;

import com.aquadist.model.Entregador;
import com.aquadist.model.Zona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregadorRepository extends JpaRepository<Entregador, Long> {

    Optional<Entregador> findByUsuarioEmailIgnoreCase(String email);

    List<Entregador> findByAtivoTrue();

    List<Entregador> findByAtivoTrueAndZona(Zona zona);
}
