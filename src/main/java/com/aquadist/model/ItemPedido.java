package com.aquadist.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "itens_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(name = "taxa_caucao_aplicada", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxaCaucaoAplicada;

    /** Quantidade de vasilhames que o cliente informou que devolvera. */
    @Column(name = "vasilhames_devolvidos", nullable = false)
    private int vasilhamesDevolvidos;

    /** Quantidade de vasilhames efetivamente aceitos na entrega (validos). */
    @Column(name = "vasilhames_validos_recebidos", nullable = false)
    private int vasilhamesValidosRecebidos;

    /** Vasilhames devolvidos porem vencidos; nao entram na troca simples (RN05). */
    @Column(name = "vasilhames_recusados_vencidos", nullable = false)
    private int vasilhamesRecusadosVencidos;

    @Column(name = "quantidade_caucao", nullable = false)
    private int quantidadeCaucao;

    @Column(name = "valor_caucao", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorCaucao = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public ItemPedido() {
    }

    public ItemPedido(Produto produto, int quantidade, int vasilhamesDevolvidos) {
        this.produto = produto;
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPrecoUnitario();
        this.taxaCaucaoAplicada = produto.getTaxaCaucao();
        this.vasilhamesDevolvidos = vasilhamesDevolvidos;
        this.vasilhamesValidosRecebidos = 0;
        this.vasilhamesRecusadosVencidos = 0;
        recalcular(vasilhamesDevolvidos);
    }

    /**
     * Recalcula subtotal e caucao considerando a quantidade de vasilhames
     * aceitos na troca. A diferenca entre o total de garrafoes e os vasilhames
     * aceitos e cobrada como caucao (RN02).
     */
    public void recalcular(int vasilhamesAceitos) {
        this.subtotal = precoUnitario.multiply(BigDecimal.valueOf(quantidade))
                .setScale(2, RoundingMode.HALF_UP);
        int faltantes = Math.max(0, quantidade - vasilhamesAceitos);
        this.quantidadeCaucao = faltantes;
        this.valorCaucao = taxaCaucaoAplicada.multiply(BigDecimal.valueOf(faltantes))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public void setPrecoUnitario(BigDecimal precoUnitario) {
        this.precoUnitario = precoUnitario;
    }

    public BigDecimal getTaxaCaucaoAplicada() {
        return taxaCaucaoAplicada;
    }

    public void setTaxaCaucaoAplicada(BigDecimal taxaCaucaoAplicada) {
        this.taxaCaucaoAplicada = taxaCaucaoAplicada;
    }

    public int getVasilhamesDevolvidos() {
        return vasilhamesDevolvidos;
    }

    public void setVasilhamesDevolvidos(int vasilhamesDevolvidos) {
        this.vasilhamesDevolvidos = vasilhamesDevolvidos;
    }

    public int getVasilhamesValidosRecebidos() {
        return vasilhamesValidosRecebidos;
    }

    public void setVasilhamesValidosRecebidos(int vasilhamesValidosRecebidos) {
        this.vasilhamesValidosRecebidos = vasilhamesValidosRecebidos;
    }

    public int getVasilhamesRecusadosVencidos() {
        return vasilhamesRecusadosVencidos;
    }

    public void setVasilhamesRecusadosVencidos(int vasilhamesRecusadosVencidos) {
        this.vasilhamesRecusadosVencidos = vasilhamesRecusadosVencidos;
    }

    public int getQuantidadeCaucao() {
        return quantidadeCaucao;
    }

    public void setQuantidadeCaucao(int quantidadeCaucao) {
        this.quantidadeCaucao = quantidadeCaucao;
    }

    public BigDecimal getValorCaucao() {
        return valorCaucao;
    }

    public void setValorCaucao(BigDecimal valorCaucao) {
        this.valorCaucao = valorCaucao;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
