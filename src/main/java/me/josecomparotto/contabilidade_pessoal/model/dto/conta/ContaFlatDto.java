package me.josecomparotto.contabilidade_pessoal.model.dto.conta;

import java.util.List;
import java.beans.Transient;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.model.enums.Natureza;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;

public class ContaFlatDto implements IDto<Conta> {
    private Integer id;
    private String codigo;
    private String descricao;
    private Integer superiorId;
    private Natureza natureza;
    private TipoConta tipo;
    private boolean redutora;
    private boolean editable;
    private boolean deletable;
    private Set<String> editableProperties;

    // saldo atual (pode ser nulo se não calculado/populado)
    private BigDecimal saldoAtual;

    @JsonIgnore
    private List<Integer> path;

    public ContaFlatDto() {}

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

    public Integer getSuperiorId() {
        return superiorId;
    }

    public void setSuperiorId(Integer superiorId) {
        this.superiorId = superiorId;
    }

    public List<Integer> getPath() {
        return path;
    }

    public void setPath(List<Integer> path) {
        this.path = path;
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

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    public void setSaldoAtual(BigDecimal saldoAtual) {
        this.saldoAtual = saldoAtual;
    }

    @Transient
    @JsonIgnore
    public String getSaldoAtualFormatado() {
        if (saldoAtual == null) {
            return "R$ 0,00";
        }
    NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return nf.format(saldoAtual);
    }

    public boolean isRedutora() {
        return redutora;
    }
    public void setRedutora(boolean redutora) {
        this.redutora = redutora;
    }

    public Boolean isEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(Boolean deletable) {
        this.deletable = deletable;
    }

    public Set<String> getEditableProperties() {
        return editableProperties;
    }
    
    public void setEditableProperties(Set<String> editableProperties) {
        this.editableProperties = editableProperties;
    }
}
