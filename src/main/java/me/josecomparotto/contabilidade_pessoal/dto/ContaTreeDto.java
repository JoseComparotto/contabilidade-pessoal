package me.josecomparotto.contabilidade_pessoal.dto;

import java.util.ArrayList;
import java.util.List;

public class ContaTreeDto {
    private Integer id;
    private String codigo;
    private String descricao;
    private List<ContaTreeDto> inferiores = new ArrayList<>();

    public ContaTreeDto() {
    }

    public ContaTreeDto(Integer id, String codigo, String descricao) {
        this.id = id;
        this.codigo = codigo;
        this.descricao = descricao;
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

    public void setInferiores(List<ContaTreeDto> inferiores) {
        this.inferiores = inferiores;
    }
}
