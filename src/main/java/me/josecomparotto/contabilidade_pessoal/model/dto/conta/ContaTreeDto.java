package me.josecomparotto.contabilidade_pessoal.model.dto.conta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.model.enums.Natureza;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoMovimento;

public class ContaTreeDto implements IDto<Conta> {
    private Integer id;
    private String codigo;
    private String descricao;
    private String displayText;
    private Natureza natureza;
    private TipoConta tipo;
    private TipoMovimento tipoMovimento;
    private Set<TipoMovimento> tiposMovimentoPossiveis;
    private final List<ContaTreeDto> inferiores = new ArrayList<>();
    private Boolean editable;
    private Boolean deletable;
    private Set<String> editableProperties;

    // saldo atual (pode ser nulo se não calculado/populado)
    private BigDecimal saldoAtual;

    public ContaTreeDto() {
    }

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

    public List<ContaTreeDto> getInferiores() {
        return inferiores;
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

    public TipoMovimento getTipoMovimento() {
        return tipoMovimento;
    }

    public Set<TipoMovimento> getTiposMovimentoPossiveis() {
        return tiposMovimentoPossiveis;
    }

    public void setTiposMovimentoPossiveis(Set<TipoMovimento> tiposMovimentoPossiveis) {
        this.tiposMovimentoPossiveis = tiposMovimentoPossiveis;
    }

    public void setTipoMovimento(TipoMovimento tipoMovimento) {
        this.tipoMovimento = tipoMovimento;
    }

    public BigDecimal getSaldoAtual() {
        return saldoAtual;
    }

    public void setSaldoAtual(BigDecimal saldoAtual) {
        this.saldoAtual = saldoAtual;
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

    public Boolean getDeletable() {
        return deletable;
    }

    public Set<String> getEditableProperties() {
        return editableProperties;
    }

    public void setEditableProperties(Set<String> editableProperties) {
        this.editableProperties = editableProperties;
    }

}
