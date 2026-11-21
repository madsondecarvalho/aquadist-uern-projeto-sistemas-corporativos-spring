package com.aquadist.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "entregadores")
public class Entregador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(length = 30)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Zona zona;

    @Column(name = "capacidade_carga", nullable = false)
    private int capacidadeCarga;

    @Column(name = "veiculo_modelo", length = 80)
    private String veiculoModelo;

    @Column(name = "veiculo_placa", length = 10)
    private String veiculoPlaca;

    @Column(nullable = false)
    private boolean ativo = true;

    public Entregador() {
    }

    public Entregador(Usuario usuario, String telefone, Zona zona, int capacidadeCarga,
                      String veiculoModelo, String veiculoPlaca) {
        this.usuario = usuario;
        this.telefone = telefone;
        this.zona = zona;
        this.capacidadeCarga = capacidadeCarga;
        this.veiculoModelo = veiculoModelo;
        this.veiculoPlaca = veiculoPlaca;
        this.ativo = true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Zona getZona() {
        return zona;
    }

    public void setZona(Zona zona) {
        this.zona = zona;
    }

    public int getCapacidadeCarga() {
        return capacidadeCarga;
    }

    public void setCapacidadeCarga(int capacidadeCarga) {
        this.capacidadeCarga = capacidadeCarga;
    }

    public String getVeiculoModelo() {
        return veiculoModelo;
    }

    public void setVeiculoModelo(String veiculoModelo) {
        this.veiculoModelo = veiculoModelo;
    }

    public String getVeiculoPlaca() {
        return veiculoPlaca;
    }

    public void setVeiculoPlaca(String veiculoPlaca) {
        this.veiculoPlaca = veiculoPlaca;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
