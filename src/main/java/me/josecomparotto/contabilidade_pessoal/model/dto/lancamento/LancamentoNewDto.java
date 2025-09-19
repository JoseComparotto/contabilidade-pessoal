package me.josecomparotto.contabilidade_pessoal.model.dto.lancamento;

import java.time.LocalDate;

import me.josecomparotto.contabilidade_pessoal.model.enums.StatusLancamento;

public class LancamentoNewDto {

    private Long id;
    private String descricao;
    private Double valor;
    private LocalDate dataCompetencia;
    
    private Integer contaDebitoId;
    private Integer contaCreditoId;

    private StatusLancamento status;

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

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public LocalDate getDataCompetencia() {
        return dataCompetencia;
    }

    public void setDataCompetencia(LocalDate dataCompetencia) {
        this.dataCompetencia = dataCompetencia;
    }

    public Integer getContaDebitoId() {
        return contaDebitoId;
    }

    public void setContaDebitoId(Integer contaDebitoId) {
        this.contaDebitoId = contaDebitoId;
    }

    public Integer getContaCreditoId() {
        return contaCreditoId;
    }

    public void setContaCreditoId(Integer contaCreditoId) {
        this.contaCreditoId = contaCreditoId;
    }

    public StatusLancamento getStatus() {
        return status;
    }

    public void setStatus(StatusLancamento status) {
        this.status = status;
    }

}
