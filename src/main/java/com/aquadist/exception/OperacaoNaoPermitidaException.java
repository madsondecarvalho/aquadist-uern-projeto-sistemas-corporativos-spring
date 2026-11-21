package com.aquadist.exception;

public class OperacaoNaoPermitidaException extends RegraDeNegocioException {

    public OperacaoNaoPermitidaException(String message) {
        super(message);
    }
}
