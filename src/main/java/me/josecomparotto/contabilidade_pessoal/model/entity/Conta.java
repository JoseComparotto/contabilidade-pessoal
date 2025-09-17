package me.josecomparotto.contabilidade_pessoal.model.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;
import me.josecomparotto.contabilidade_pessoal.model.enums.StatusLancamento;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;

@Entity
@Table(name = "tb_contas", schema = "public")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = true)
    @JoinColumn(name = "id_superior")
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
    private final List<Conta> inferiores = new ArrayList<>();

    @Column(name = "aceita_movimento_oposto")
    private Boolean aceitaMovimentoOposto;

    @Column(name = "ativa")
    private Boolean ativa;

    @Column(name = "created_by_system")
    private Boolean createdBySystem;

    @OneToMany(mappedBy = "contaDebito")
    private final List<Lancamento> lancamentosDebito = new ArrayList<>();

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

            if (canEditTipo()) {
                editableProperties.add("tipo");
            }

            if (canEditRedutora()) {
                editableProperties.add("redutora");
            }
        }

        // Retorna uma cópia imutável do conjunto de propriedades editáveis
        return Set.copyOf(editableProperties);
    }

    @Transient
    public boolean isAceitaSentido(SentidoContabil sentido) {
        if (sentido == null) {
            return false;
        }
        if (Natureza.CREDORA.equals(natureza) && SentidoContabil.CREDITO.equals(sentido)) {
            return true;
        }
        if (Natureza.DEVEDORA.equals(natureza) && SentidoContabil.DEBITO.equals(sentido)) {
            return true;
        }
        return aceitaMovimentoOposto;
    }

    private boolean canEditTipo() {
        // Regras consideradas:
        // - O tipo só pode ser alterado se a conta não tiver inferiores nem lançamentos
        // diretos.
        return inferiores.isEmpty() && lancamentosDebito.isEmpty() && lancamentosCredito.isEmpty();
    }

    private boolean canEditRedutora(){
        if(isRedutora()){
            // Pode deixar de ser redutora se a superior não for redutora
            return superior == null || !superior.isRedutora();
        } else {
            // Pode se tornar redutora se todas as inferiores forem redutoras
            return inferiores.stream().allMatch(Conta::isRedutora);
        }
    }

    @Transient
    public boolean isEditable() {
        // Uma conta pode ser editada se não for uma conta criada pelo sistema
        return !Boolean.TRUE.equals(createdBySystem);
    }

    @Transient
    public boolean isDeletable() {
        // Uma conta pode ser deletada se não tiver inferiores, lançamentos e não for
        // uma conta
        // criada pelo sistema
        return inferiores.isEmpty() && lancamentosDebito.isEmpty() && lancamentosCredito.isEmpty()
                && !Boolean.TRUE.equals(createdBySystem);
    }

    @Transient
    public Conta getRaiz() {
        Conta current = this;
        while (current.getSuperior() != null) {
            current = current.getSuperior();
        }
        return current;
    }

    @Transient
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

    @Transient
    public boolean isRedutora() {
        Natureza raiz = getRaiz() != null ? getRaiz().getNatureza() : null;
        Natureza atual = this.natureza;
        // Redutora quando a natureza difere da raiz; nulls tratados como não redutora
        return (raiz != null && atual != null) && !atual.equals(raiz);
    }

    @Transient
    public void setRedutora(boolean redu) {
        Natureza raiz = getRaiz() != null ? getRaiz().getNatureza() : null;
        if (raiz == null) {
            throw new IllegalStateException("Não é possível definir a natureza redutora.");
        }
        this.natureza = redu ? naturezaOposta(raiz) : raiz;
    }

    @Transient
    public BigDecimal getSaldoNatural() {
        Natureza n = getNatureza();
        if (n == null) {
            return BigDecimal.ZERO;
        }
        switch (n) {
            case CREDORA:
                return getSaldoContabil();
            case DEVEDORA:
                return getSaldoContabil().multiply(BigDecimal.valueOf(-1));
            default:
                return BigDecimal.ZERO;
        }
    }

    @Transient
    public BigDecimal getSaldoContabil() {

        switch (getTipo()) {
            // Se for analítica, o saldo é o somatório líquido dos lançamentos efetivos
            case ANALITICA:
                BigDecimal saldoDebito = lancamentosDebito.stream()
                        .filter(l -> StatusLancamento.EFETIVO.equals(l.getStatus()))
                        .map(Lancamento::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal saldoCredito = lancamentosCredito.stream()
                        .filter(l -> StatusLancamento.EFETIVO.equals(l.getStatus()))
                        .map(Lancamento::getValor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                return saldoCredito.subtract(saldoDebito);

            // Se for sintética, o saldo é o somatório dos saldos das contas inferiores
            // analíticas
            case SINTETICA:
                return getTodasInferiores().stream()
                        .filter(c -> TipoConta.ANALITICA.equals(c.getTipo()))
                        .map(Conta::getSaldoContabil)
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

    /**
     * Define a conta superior.
     * <p>
     * Atenção: Este método propaga automaticamente o valor da propriedade
     * aceitaMovimentoOposto da conta superior para esta conta, caso a conta
     * superior não seja nula.
     * Esse efeito colateral pode impactar regras de negócio e deve ser considerado
     * ao utilizar este setter.
     *
     * @param superior a conta superior a ser definida
     */
    public void setSuperior(Conta superior) {
        this.superior = superior;

        // Propagar aceitação de movimento oposto
        if (superior != null) {
            this.aceitaMovimentoOposto = superior.getAceitaMovimentoOposto();
        }
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

    public Boolean getAceitaMovimentoOposto() {
        return aceitaMovimentoOposto;
    }

    public void setAceitaMovimentoOposto(Boolean aceitaMovimentoOposto) {
        this.aceitaMovimentoOposto = aceitaMovimentoOposto;
    }

    public Boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(Boolean ativa) {
        this.ativa = ativa;
    }

    @Override
    public String toString() {
        return getDisplayText();
    }

}
