package com.aquadist.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregador_id")
    private Entregador entregador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedido status = StatusPedido.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", nullable = false, length = 30)
    private FormaPagamento formaPagamento;

    @Column(name = "pagamento_confirmado")
    private Boolean pagamentoConfirmado = Boolean.FALSE;

    @Column(name = "pago_em")
    private LocalDateTime pagoEm;

    @Embedded
    private Endereco enderecoEntrega;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ItemPedido> itens = new ArrayList<>();

    @Column(name = "valor_produtos", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorProdutos = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "valor_caucao", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorCaucao = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(length = 500)
    private String observacao;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "despachado_em")
    private LocalDateTime despachadoEm;

    @Column(name = "entregue_em")
    private LocalDateTime entregueEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;

    @Version
    private Long version;

    public Pedido() {
    }

    public Pedido(Cliente cliente, Endereco enderecoEntrega, FormaPagamento formaPagamento, String observacao) {
        this.cliente = cliente;
        this.enderecoEntrega = enderecoEntrega;
        this.formaPagamento = formaPagamento;
        this.observacao = observacao;
        this.status = StatusPedido.PENDENTE;
    }

    @PrePersist
    void aoCriar() {
        LocalDateTime agora = LocalDateTime.now();
        if (criadoEm == null) {
            criadoEm = agora;
        }
        atualizadoEm = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = LocalDateTime.now();
    }

    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        this.itens.add(item);
    }

    public void recalcularTotais() {
        BigDecimal produtos = itens.stream()
                .map(ItemPedido::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal caucao = itens.stream()
                .map(ItemPedido::getValorCaucao)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.valorProdutos = produtos.setScale(2, RoundingMode.HALF_UP);
        this.valorCaucao = caucao.setScale(2, RoundingMode.HALF_UP);
        this.valorTotal = this.valorProdutos.add(this.valorCaucao).setScale(2, RoundingMode.HALF_UP);
    }

    public int totalGarrafoes() {
        return itens.stream().mapToInt(ItemPedido::getQuantidade).sum();
    }

    public boolean podeCancelar() {
        return status.podeCancelar();
    }

    public Zona getZonaEntrega() {
        return enderecoEntrega == null ? null : enderecoEntrega.getZona();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public StatusPedido getStatus() {
        return status;
    }

    public void setStatus(StatusPedido status) {
        this.status = status;
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(FormaPagamento formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public boolean isPagamentoConfirmado() {
        return Boolean.TRUE.equals(pagamentoConfirmado);
    }

    public void setPagamentoConfirmado(boolean pagamentoConfirmado) {
        this.pagamentoConfirmado = pagamentoConfirmado;
    }

    public LocalDateTime getPagoEm() {
        return pagoEm;
    }

    public void setPagoEm(LocalDateTime pagoEm) {
        this.pagoEm = pagoEm;
    }

    public Endereco getEnderecoEntrega() {
        return enderecoEntrega;
    }

    public void setEnderecoEntrega(Endereco enderecoEntrega) {
        this.enderecoEntrega = enderecoEntrega;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedido> itens) {
        this.itens = itens;
    }

    public BigDecimal getValorProdutos() {
        return valorProdutos;
    }

    public void setValorProdutos(BigDecimal valorProdutos) {
        this.valorProdutos = valorProdutos;
    }

    public BigDecimal getValorCaucao() {
        return valorCaucao;
    }

    public void setValorCaucao(BigDecimal valorCaucao) {
        this.valorCaucao = valorCaucao;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    public LocalDateTime getDespachadoEm() {
        return despachadoEm;
    }

    public void setDespachadoEm(LocalDateTime despachadoEm) {
        this.despachadoEm = despachadoEm;
    }

    public LocalDateTime getEntregueEm() {
        return entregueEm;
    }

    public void setEntregueEm(LocalDateTime entregueEm) {
        this.entregueEm = entregueEm;
    }

    public LocalDateTime getCanceladoEm() {
        return canceladoEm;
    }

    public void setCanceladoEm(LocalDateTime canceladoEm) {
        this.canceladoEm = canceladoEm;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public Long getVersion() {
        return version;
    }
}
