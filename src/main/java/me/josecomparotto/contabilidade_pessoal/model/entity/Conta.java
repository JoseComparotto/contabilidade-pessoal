package me.josecomparotto.contabilidade_pessoal.model.entity;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import me.josecomparotto.contabilidade_pessoal.application.converter.NaturezaConverter;
import me.josecomparotto.contabilidade_pessoal.application.converter.TipoContaConverter;
import me.josecomparotto.contabilidade_pessoal.model.enums.Natureza;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;

@Entity
@Table(name = "tb_contas", schema = "public")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = true)
    @JoinColumn(name = "id_superior")
    @JsonIgnoreProperties("inferiores")
    private Conta superior;

    private Integer sequencia;

    private String descricao;

    @Column(name = "credora")
    @Convert(converter = NaturezaConverter.class)
    private Natureza natureza;

    @Column(name = "analitica")
    @Convert(converter = TipoContaConverter.class)
    private TipoConta tipo;

    @OneToMany(mappedBy = "superior")
    @JsonIgnoreProperties("superior")
    private final List<Conta> inferiores = new ArrayList<>();

    private Boolean createdBySystem;

    @Transient
    public String getCodigo() {
        return getPath().stream()
                .map(seq -> String.format("%d", seq))
                .reduce((a, b) -> a + "." + b)
                .orElse("");
    }

    @Transient
    public List<Integer> getPath() {
        List<Integer> path = new ArrayList<>();

        Conta current = this;
        while (current != null) {
            path.add(0, current.getSequencia());
            current = current.getSuperior();
        }
        return path;
    }

    @Transient
    public boolean isDeletable() {
        // Uma conta pode ser deletada se não tiver inferiores e não for uma conta criada pelo sistema
        return inferiores.isEmpty() && !Boolean.TRUE.equals(createdBySystem);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Conta getSuperior() {
        return superior;
    }

    public void setSuperior(Conta superior) {
        this.superior = superior;
    }

    public Integer getSequencia() {
        return sequencia;
    }

    public void setSequencia(Integer sequencia) {
        this.sequencia = sequencia;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
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

    public List<Conta> getInferiores() {
        return inferiores;
    }

    public Boolean getCreatedBySystem() {
        return createdBySystem;
    }

    public void setCreatedBySystem(Boolean createdBySystem) {
        this.createdBySystem = createdBySystem;
    }

}
