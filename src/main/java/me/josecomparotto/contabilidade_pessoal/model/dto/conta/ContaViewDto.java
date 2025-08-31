package me.josecomparotto.contabilidade_pessoal.model.dto.conta;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.model.enums.Natureza;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;

public class ContaViewDto implements IDto<Conta> {
    private Integer id;
    private String codigo;
    private String descricao;
    private String displayText;
    private BigDecimal saldoAtual;
    private Natureza natureza;
    private TipoConta tipo;
    private boolean redutora;
    private boolean aceitaMovimentoOposto;
    private boolean editable;
    private boolean deletable;
    private Set<String> editableProperties;

    @JsonIgnoreProperties({ "superior", "inferiores", "lancamentos" })
    private ContaViewDto superior;

    @JsonIgnoreProperties({ "superior", "inferiores", "lancamentos" })
    private List<ContaViewDto> inferiores;

    @JsonIgnoreProperties({ "contaPartida", "inferiores" })
    private List<LancamentoPartidaDto> lancamentos;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    @JsonIgnore
    public String getSaldoAtualFormatado() {
        if (saldoAtual == null || BigDecimal.ZERO.compareTo(saldoAtual) == 0) {
            return "R$ 0,00";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return nf.format(saldoAtual);
    }

    public void setSaldoAtual(BigDecimal saldoAtual) {
        this.saldoAtual = saldoAtual;
    }

    public Natureza getNatureza() {
        return natureza;
    }

    public void setNatureza(Natureza natureza) {
        this.natureza = natureza;
    }

    public TipoConta getTipo() {
        return tipo;
    }

    public void setTipo(TipoConta tipo) {
        this.tipo = tipo;
    }

    public boolean isRedutora() {
        return redutora;
    }

    public void setRedutora(boolean redutora) {
        this.redutora = redutora;
    }

    public boolean isAceitaMovimentoOposto() {
        return aceitaMovimentoOposto;
    }

    public void setAceitaMovimentoOposto(boolean aceitaMovimentoOposto) {
        this.aceitaMovimentoOposto = aceitaMovimentoOposto;
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

    public Set<String> getEditableProperties() {
        return editableProperties;
    }

    public void setEditableProperties(Set<String> editableProperties) {
        this.editableProperties = editableProperties;
    }

    public ContaViewDto getSuperior() {
        return superior;
    }

    public void setSuperior(ContaViewDto superior) {
        this.superior = superior;
    }

    public List<ContaViewDto> getInferiores() {
        return inferiores;
    }

    public void setInferiores(List<ContaViewDto> inferiores) {
        this.inferiores = inferiores;
    }

    public List<LancamentoPartidaDto> getLancamentos() {
        return lancamentos;
    }

    public void setLancamentos(List<LancamentoPartidaDto> lancamentos) {
        this.lancamentos = lancamentos;
    }

    
}
