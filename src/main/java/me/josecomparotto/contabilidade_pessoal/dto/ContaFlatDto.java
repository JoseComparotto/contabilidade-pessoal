package me.josecomparotto.contabilidade_pessoal.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ContaFlatDto {
    private Integer id;
    private String codigo;
    private String descricao;
    private Integer superiorId;

    @JsonIgnore
    private List<Integer> path;

    public ContaFlatDto() {}

    public ContaFlatDto(Integer id, String codigo, String descricao, Integer superiorId, List<Integer> path) {
        this.id = id;
        this.codigo = codigo;
        this.descricao = descricao;
        this.superiorId = superiorId;
        this.path = path;
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

}
