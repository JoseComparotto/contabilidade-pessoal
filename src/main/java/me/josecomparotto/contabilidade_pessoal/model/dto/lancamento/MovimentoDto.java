package me.josecomparotto.contabilidade_pessoal.model.dto.lancamento;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoNatural;
import me.josecomparotto.contabilidade_pessoal.model.enums.StatusLancamento;

public class MovimentoDto {

    // Segmentação
    private StatusLancamento status;
    private boolean agregado;

    // Dados comuns
    private LocalDate data;
    private String descricao;
    private Double valor;
    private Double saldo;

    // Dados do lancamento
    private Long idLancamento;
    private SentidoContabil sentidoContabil;
    private SentidoNatural sentidoNatural;
    private ContaViewDto contaPartida;
    private ContaViewDto contaContrapartida;

    public StatusLancamento getStatus() {
        return status;
    }

    public void setStatus(StatusLancamento status) {
        this.status = status;
    }

    public boolean isAgregado() {
        return agregado;
    }

    public void setAgregado(boolean agregado) {
        this.agregado = agregado;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Double getSaldo() {
        return saldo;
    }

    public void setSaldo(Double saldo) {
        this.saldo = saldo;
    }

    public Long getIdLancamento() {
        return idLancamento;
    }

    public void setIdLancamento(Long idLancamento) {
        this.idLancamento = idLancamento;
    }

    public SentidoContabil getSentidoContabil() {
        return sentidoContabil;
    }

    public void setSentidoContabil(SentidoContabil sentidoContabil) {
        this.sentidoContabil = sentidoContabil;
    }

    public SentidoNatural getSentidoNatural() {
        return sentidoNatural;
    }

    public void setSentidoNatural(SentidoNatural sentidoNatural) {
        this.sentidoNatural = sentidoNatural;
    }

    public ContaViewDto getContaPartida() {
        return contaPartida;
    }

    public void setContaPartida(ContaViewDto contaPartida) {
        this.contaPartida = contaPartida;
    }

    public ContaViewDto getContaContrapartida() {
        return contaContrapartida;
    }

    public void setContaContrapartida(ContaViewDto contaContrapartida) {
        this.contaContrapartida = contaContrapartida;
    }

    @JsonIgnore
    public String getDataFormatada() {
        return data != null ? data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    @JsonIgnore
    public String getValorFormatado() {
        if (valor == null || Double.compare(valor, 0.0) == 0) {
            return "R$ 0,00";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return nf.format(valor);
    }

    @JsonIgnore
    public String getSaldoFormatado() {
        if (saldo == null || Double.compare(saldo, 0.0) == 0) {
            return "R$ 0,00";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return nf.format(saldo);
    }
}
