package com.aquadist.model;

public enum StatusPedido {
    PENDENTE,
    ATRIBUIDO,
    EM_TRANSITO,
    ENTREGUE,
    CANCELADO;

    /** RN04: o cliente so pode cancelar antes do despacho (Em Transito). */
    public boolean podeCancelar() {
        return this == PENDENTE || this == ATRIBUIDO;
    }

    public boolean emAberto() {
        return this == PENDENTE || this == ATRIBUIDO || this == EM_TRANSITO;
    }
}
