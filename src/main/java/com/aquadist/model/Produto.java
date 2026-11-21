package com.aquadist.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "volume_litros", nullable = false)
    private int volumeLitros;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "taxa_caucao", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxaCaucao;

    @Column(nullable = false)
    private boolean ativo = true;

    public Produto() {
    }

    public Produto(String codigo, String nome, int volumeLitros, BigDecimal precoUnitario, BigDecimal taxaCaucao) {
        this.codigo = codigo;
        this.nome = nome;
        this.volumeLitros = volumeLitros;
        this.precoUnitario = precoUnitario;
        this.taxaCaucao = taxaCaucao;
        this.ativo = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public int getVolumeLitros() {
        return volumeLitros;
    }

    public void setVolumeLitros(int volumeLitros) {
        this.volumeLitros = volumeLitros;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getTaxaCaucao() {
        return taxaCaucao;
    }

    public void setTaxaCaucao(BigDecimal taxaCaucao) {
        this.taxaCaucao = taxaCaucao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
