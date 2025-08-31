package me.josecomparotto.contabilidade_pessoal.model.dto.conta;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;

public class ContaNewDto implements IDto<Conta> {
    private Integer superiorId;
    private String descricao;
    private TipoConta tipo;
    private Boolean redutora;

    public ContaNewDto() {
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

    public TipoConta getTipo() {
        return tipo;
    }

    public void setTipo(TipoConta tipo) {
        this.tipo = tipo;
    }

    public Boolean isRedutora() {
        return redutora;
    }

    public void setRedutora(Boolean redutora) {
        this.redutora = redutora;
    }

}
