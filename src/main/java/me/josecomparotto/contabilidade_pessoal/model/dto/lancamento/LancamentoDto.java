package me.josecomparotto.contabilidade_pessoal.model.dto.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;

public class LancamentoDto implements IDto<Lancamento> {

    private Long id;
    private String descricao;
    private BigDecimal valor;
    private LocalDate dataCompetencia;

    @JsonIgnoreProperties({"superior", "inferiores"})
    private ContaViewDto contaDebito;

    @JsonIgnoreProperties({"superior", "inferiores"})
    private ContaViewDto contaCredito;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getDataCompetencia() {
        return dataCompetencia;
    }

    public void setDataCompetencia(LocalDate dataCompetencia) {
        this.dataCompetencia = dataCompetencia;
    }

    public ContaViewDto getContaDebito() {
        return contaDebito;
    }

    public void setContaDebito(ContaViewDto contaDebito) {
        this.contaDebito = contaDebito;
    }

    public ContaViewDto getContaCredito() {
        return contaCredito;
    }

    public void setContaCredito(ContaViewDto contaCredito) {
        this.contaCredito = contaCredito;
    }
}
