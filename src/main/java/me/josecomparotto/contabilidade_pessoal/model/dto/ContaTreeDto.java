package me.josecomparotto.contabilidade_pessoal.model.dto;

import java.util.ArrayList;
import java.util.List;

import me.josecomparotto.contabilidade_pessoal.model.enums.Natureza;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;

public class ContaTreeDto {
    private Integer id;
    private String codigo;
    private String descricao;
    private Natureza natureza;
    private TipoConta tipo;
    private final List<ContaTreeDto> inferiores = new ArrayList<>();

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

}
