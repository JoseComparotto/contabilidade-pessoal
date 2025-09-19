package me.josecomparotto.contabilidade_pessoal.model.dto.lancamento;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonIgnore;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.enums.StatusLancamento;

public class LancamentoDto implements IDto<Lancamento> {

    private Long id;
    private String descricao;
    private Double valor;
    private LocalDate dataCompetencia;
    private ContaViewDto contaDebito;
    private ContaViewDto contaCredito;
    private StatusLancamento status;

    private boolean editable;
    private boolean deletable;

    private String displayText;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getDataCompetencia() {
        return dataCompetencia;
    }

    public void setDataCompetencia(LocalDate dataCompetencia) {
        this.dataCompetencia = dataCompetencia;
    }

    public ContaViewDto getContaDebito() {
        return contaDebito;
    }

    public void setContaDebito(ContaViewDto contaDebito) {
        this.contaDebito = contaDebito;
    }

    public ContaViewDto getContaCredito() {
        return contaCredito;
    }

    public void setContaCredito(ContaViewDto contaCredito) {
        this.contaCredito = contaCredito;
    }

    public StatusLancamento getStatus() {
        return status;
    }
    public void setStatus(StatusLancamento status) {
        this.status = status;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    @JsonIgnore
    public String getDataCompetenciaFormatada() {
        return dataCompetencia != null ? dataCompetencia.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    @JsonIgnore
    public String getValorFormatado() {
        if (valor == null || Double.compare(valor, 0.0) == 0) {
            return "-";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.forLanguageTag("pt-BR"));
        DecimalFormat df = new DecimalFormat("#,##0.00;(#,##0.00)", symbols);
        return df.format(valor);
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}
