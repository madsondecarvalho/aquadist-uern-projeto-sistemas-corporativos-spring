package com.aquadist.model;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "clientes")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCliente tipo;

    @Column(nullable = false, unique = true, length = 20)
    private String documento;

    @Column(length = 30)
    private String telefone;

    @Embedded
    private Endereco endereco;

    @Column(name = "limite_credito", nullable = false, precision = 12, scale = 2)
    private BigDecimal limiteCredito = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(name = "saldo_devedor", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoDevedor = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(nullable = false)
    private boolean ativo = true;

    public Cliente() {
    }

    public Cliente(Usuario usuario, TipoCliente tipo, String documento, String telefone,
                   Endereco endereco, BigDecimal limiteCredito) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.documento = documento;
        this.telefone = telefone;
        this.endereco = endereco;
        this.limiteCredito = limiteCredito == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : limiteCredito.setScale(2, RoundingMode.HALF_UP);
        this.saldoDevedor = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.ativo = true;
    }

    public boolean isCorporativo() {
        return tipo == TipoCliente.CORPORATIVO;
    }

    /**
     * RN01: clientes corporativos nao podem ultrapassar o limite mensal.
     * O valor em aberto (pedidos ainda nao entregues) tambem compromete o limite.
     */
    public boolean permiteCompra(BigDecimal valorPedido, BigDecimal valorPedidosEmAberto) {
        if (!isCorporativo()) {
            return true;
        }
        BigDecimal comprometido = saldoDevedor
                .add(valorPedidosEmAberto == null ? BigDecimal.ZERO : valorPedidosEmAberto)
                .add(valorPedido);
        return comprometido.compareTo(limiteCredito) <= 0;
    }

    public BigDecimal getLimiteDisponivel() {
        return limiteCredito.subtract(saldoDevedor).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public void adicionarDebito(BigDecimal valor) {
        this.saldoDevedor = this.saldoDevedor.add(valor).setScale(2, RoundingMode.HALF_UP);
    }

    public void registrarPagamento(BigDecimal valor) {
        this.saldoDevedor = this.saldoDevedor.subtract(valor)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
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

    public TipoCliente getTipo() {
        return tipo;
    }

    public void setTipo(TipoCliente tipo) {
        this.tipo = tipo;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Endereco getEndereco() {
        return endereco;
    }

    public void setEndereco(Endereco endereco) {
        this.endereco = endereco;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(BigDecimal limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public BigDecimal getSaldoDevedor() {
        return saldoDevedor;
    }

    public void setSaldoDevedor(BigDecimal saldoDevedor) {
        this.saldoDevedor = saldoDevedor;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
