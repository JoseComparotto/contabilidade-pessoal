package me.josecomparotto.contabilidade_pessoal.model.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.Transient;

import me.josecomparotto.contabilidade_pessoal.application.converter.NaturezaConverter;
import me.josecomparotto.contabilidade_pessoal.application.converter.TipoContaConverter;
import me.josecomparotto.contabilidade_pessoal.model.enums.Natureza;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoMovimento;

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

    @Column(name = "sequencia")
    private Integer sequencia;

    @Column(name = "descricao")
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

    @Column(name = "aceita_movimento_oposto")
    private Boolean aceitaMovimentoOposto;

    @Column(name = "created_by_system")
    private Boolean createdBySystem;

    @JsonIgnore
    @OneToMany(mappedBy = "contaDebito")
    private final List<Lancamento> lancamentosDebito = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "contaCredito")
    private final List<Lancamento> lancamentosCredito = new ArrayList<>();

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
    public Set<String> getEditableProperties() {
        Set<String> editableProperties = new HashSet<>();
        if (isEditable()) {
            editableProperties.add("descricao");

            if (canEditTipoMovimento()) {
                editableProperties.add("tipoMovimento");
            }

            if (inferiores.isEmpty()) {
                // Só permite alterar o tipo se a conta não tiver inferiores
                editableProperties.add("tipo");
            }
        }

        // Retorna uma cópia imutável do conjunto de propriedades editáveis
        return Set.copyOf(editableProperties);
    }

    private boolean canEditTipoMovimento() {

        Set<TipoMovimento> tiposInferiores = getTodasInferiores().stream()
                .map(Conta::getTipoMovimento)
                .collect(Collectors.toSet());

        boolean algumaInferiorDiferente = tiposInferiores.stream()
                .anyMatch(t -> !Objects.equals(t, getTipoMovimento()));

        // Se a atual não é mista e existe alguma conta inferior diferente, então não
        // aceita
        if (!TipoMovimento.MISTO.equals(getTipoMovimento()) && algumaInferiorDiferente) {
            return false;
        }

        // Se a atual for mista e existem ao menos uma conta inferior mista, então não
        // aceita
        if (TipoMovimento.MISTO.equals(getTipoMovimento()) && tiposInferiores.contains(TipoMovimento.MISTO)) {
            return false;
        }

        // Se a atual for mista e existir tanto inferior natual quanto redutor, então
        // não aceita
        if (TipoMovimento.MISTO.equals(getTipoMovimento())
                && tiposInferiores.contains(TipoMovimento.NATURAL)
                && tiposInferiores.contains(TipoMovimento.REDUTOR)) {
            return false;
        }

        // Se a conta superior for mista, então aceita
        if (superior != null && TipoMovimento.MISTO.equals(superior.getTipoMovimento())) {
            return true;
        }

        return false;
    }

    @Transient
    public boolean isEditable() {
        // Uma conta pode ser editada se não for uma conta criada pelo sistema
        return !Boolean.TRUE.equals(createdBySystem);
    }

    @Transient
    public boolean isDeletable() {
        // Uma conta pode ser deletada se não tiver inferiores e não for uma conta
        // criada pelo sistema
        return inferiores.isEmpty() && !Boolean.TRUE.equals(createdBySystem);
    }

    @Transient
    @JsonIgnore
    public Conta getRaiz() {
        Conta current = this;
        while (current.getSuperior() != null) {
            current = current.getSuperior();
        }
        return current;
    }

    @Transient
    @JsonIgnore
    public List<Conta> getTodasInferiores() {
        List<Conta> todasInferiores = new ArrayList<>();
        List<Conta> stack = new ArrayList<>();
        for (Conta inferior : inferiores) {
            stack.add(inferior);
        }

        Set<Conta> visited = new HashSet<>();

        while (!stack.isEmpty()) {
            Conta current = stack.remove(stack.size() - 1); // pop
            if (!visited.add(current)) {
                continue;
            }
            todasInferiores.add(current);

            List<Conta> children = current.getInferiores();
            for (Conta child : children) {
                stack.add(child);
            }
        }
        return todasInferiores;
    }

    private boolean isRedutora() {
        Natureza raiz = getRaiz() != null ? getRaiz().getNatureza() : null;
        Natureza atual = this.natureza;
        // Redutora quando a natureza difere da raiz; nulls tratados como não redutora
        return (raiz != null && atual != null) && !atual.equals(raiz);
    }

    @Transient
    public TipoMovimento getTipoMovimento() {
        if (isRedutora()) {
            return TipoMovimento.REDUTOR;
        } else if (Boolean.TRUE.equals(aceitaMovimentoOposto)) {
            return TipoMovimento.MISTO;
        } else {
            return TipoMovimento.NATURAL;
        }
    }

    @Transient
    public void setTipoMovimento(TipoMovimento tipoMovimento) {
        switch (tipoMovimento) {
            case REDUTOR:
                this.natureza = naturezaOposta(getRaiz().getNatureza());
                this.aceitaMovimentoOposto = false;
                break;
            case MISTO:
                this.natureza = getRaiz().getNatureza();
                this.aceitaMovimentoOposto = true;
                break;
            case NATURAL:
                this.natureza = getRaiz().getNatureza();
                this.aceitaMovimentoOposto = false;
                break;
            default:
                // No-op
                break;
        }
    }

    @Transient
    public BigDecimal getSaldoNatural() {
        Natureza n = getNatureza();
        if (n == null) {
            return BigDecimal.ZERO;
        }
        switch (n) {
            case CREDORA:
                return getSaldoMatematico();
            case DEVEDORA:
                return getSaldoMatematico().multiply(BigDecimal.valueOf(-1));
            default:
                return BigDecimal.ZERO;
        }
    }

    @Transient
    public BigDecimal getSaldoMatematico() {

        switch (getTipo()) {
            // Se for analítica, o saldo é o somatório líquido dos lançamentos
            case ANALITICA:
                BigDecimal saldoDebito = lancamentosDebito.stream()
                        .map(Lancamento::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal saldoCredito = lancamentosCredito.stream()
                        .map(Lancamento::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return saldoCredito.subtract(saldoDebito);

            // Se for sintética, o saldo é o somatório dos saldos das contas inferiores
            // analíticas
            case SINTETICA:
                return getTodasInferiores().stream()
                        .filter(c -> TipoConta.ANALITICA.equals(c.getTipo()))
                        .map(Conta::getSaldoMatematico)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            default:
                break;
        }

        return BigDecimal.ZERO;
    }

    @Transient
    public String getDisplayText() {
        Conta raiz = getRaiz();

        return String.format("%s. %s%s%s",
                getCodigo(),
                isRedutora() ? "(-) " : "",
                getDescricao(),
                !this.equals(raiz) ? String.format(" (%s)", raiz.getDescricao()) : "");
    }

    private static Natureza naturezaOposta(Natureza natureza) {
        return natureza.equals(Natureza.DEVEDORA) ? Natureza.CREDORA : Natureza.DEVEDORA;
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

    public List<Lancamento> getLancamentosDebito() {
        return lancamentosDebito;
    }

    public List<Lancamento> getLancamentosCredito() {
        return lancamentosCredito;
    }

}
