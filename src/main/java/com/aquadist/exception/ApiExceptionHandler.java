package com.aquadist.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail recursoEstaticoNaoEncontrado(NoResourceFoundException ex) {
        return problema(HttpStatus.NOT_FOUND, "Recurso nao encontrado",
                "O recurso solicitado nao existe: " + ex.getResourcePath());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Parametro invalido",
                "O valor informado para '" + ex.getName() + "' nao e valido.");
    }

    @ExceptionHandler(CreditoExcedidoException.class)
    ProblemDetail credito(CreditoExcedidoException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "Limite de credito excedido", ex.getMessage());
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    ProblemDetail estoque(EstoqueInsuficienteException ex) {
        return problema(HttpStatus.CONFLICT, "Estoque insuficiente", ex.getMessage());
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    ProblemDetail operacao(OperacaoNaoPermitidaException ex) {
        return problema(HttpStatus.CONFLICT, "Operacao nao permitida", ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    ProblemDetail regra(RegraDeNegocioException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Regra de negocio violada", ex.getMessage());
    }

    @ExceptionHandler({AcessoNegadoException.class, AccessDeniedException.class})
    ProblemDetail acessoNegado(Exception ex) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException ex) {
        ProblemDetail pd = problema(HttpStatus.BAD_REQUEST, "Erro de validacao",
                "Um ou mais campos estao invalidos.");
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.put(erro.getField(), erro.getDefaultMessage());
        }
        pd.setProperty("campos", campos);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail jsonInvalido(HttpMessageNotReadableException ex) {
        return problema(HttpStatus.BAD_REQUEST, "JSON invalido",
                "Nao foi possivel interpretar o corpo da requisicao. Verifique tipos e valores de enum.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException ex) {
        log.warn("Violacao de integridade: {}", ex.getMessage());
        return problema(HttpStatus.CONFLICT, "Conflito de dados",
                "A operacao viola uma restricao de unicidade ou integridade dos dados.");
    }

    @ExceptionHandler(ClientAbortException.class)
    void conexaoAbortada(ClientAbortException ex) {
        // O navegador fechou/cancelou a requisicao (comum ao recarregar paginas ou fechar abas).
        log.debug("Cliente interrompeu a conexao: {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Consulte os logs da aplicacao.");
    }

    private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detalhe);
        pd.setTitle(titulo);
        pd.setType(URI.create("https://aquadist.local/errors/" + status.value()));
        return pd;
    }
}
