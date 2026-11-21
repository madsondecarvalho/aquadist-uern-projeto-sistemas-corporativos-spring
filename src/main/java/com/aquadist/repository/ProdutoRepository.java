package com.aquadist.repository;

import com.aquadist.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    List<Produto> findByAtivoTrueOrderByVolumeLitrosDesc();

    Optional<Produto> findByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<Produto> findByIdAndAtivoTrue(Long id);
}
