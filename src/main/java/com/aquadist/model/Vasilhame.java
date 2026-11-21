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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Representa cada garrafao fisico, com codigo e validade individual.
 * A rastreabilidade permite aplicar a RN05 na troca de vasilhames.
 */
@Entity
@Table(name = "vasilhames")
public class Vasilhame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(name = "data_validade", nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusVasilhame status = StatusVasilhame.NA_CENTRAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregador_id")
    private Entregador entregador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    public Vasilhame() {
    }

    public Vasilhame(String codigo, Produto produto, LocalDate dataValidade) {
        this.codigo = codigo;
        this.produto = produto;
        this.dataValidade = dataValidade;
        this.status = StatusVasilhame.NA_CENTRAL;
    }

    public boolean estaVencido(LocalDate referencia) {
        return dataValidade.isBefore(referencia);
    }

    public boolean disponivelParaTroca(LocalDate hoje) {
        return status == StatusVasilhame.COM_CLIENTE && !estaVencido(hoje);
    }

    public void carregarParaTransito(Entregador entregador, Pedido pedido) {
        this.status = StatusVasilhame.EM_TRANSITO;
        this.entregador = entregador;
        this.pedido = pedido;
        this.cliente = null;
    }

    public void entregarAoCliente(Cliente cliente) {
        this.status = StatusVasilhame.COM_CLIENTE;
        this.cliente = cliente;
        this.entregador = null;
        this.pedido = null;
    }

    public void retornarACentral() {
        this.status = StatusVasilhame.NA_CENTRAL;
        this.cliente = null;
        this.entregador = null;
        this.pedido = null;
    }

    public void descartar() {
        this.status = StatusVasilhame.DESCARTADO;
        this.cliente = null;
        this.entregador = null;
        this.pedido = null;
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

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public StatusVasilhame getStatus() {
        return status;
    }

    public void setStatus(StatusVasilhame status) {
        this.status = status;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Entregador getEntregador() {
        return entregador;
    }

    public void setEntregador(Entregador entregador) {
        this.entregador = entregador;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }
}
