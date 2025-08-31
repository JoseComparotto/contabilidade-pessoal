package me.josecomparotto.contabilidade_pessoal.model.dto.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoOperacao;

public class LancamentoPartidaDto implements IDto<Lancamento> {

    private Long id;
    private String descricao;
    private LocalDate dataCompetencia;

    private ContaViewDto contaPartida;
    private ContaViewDto contaContrapartida;

    private SentidoOperacao sentido;
    private BigDecimal valorMatematico; // negativo quando sentido = DEBITO

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

    public LocalDate getDataCompetencia() {
        return dataCompetencia;
    }

    public void setDataCompetencia(LocalDate dataCompetencia) {
        this.dataCompetencia = dataCompetencia;
    }

    public ContaViewDto getContaPartida() {
        return contaPartida;
    }

    public void setContaPartida(ContaViewDto contaPartida) {
        this.contaPartida = contaPartida;
    }

    public ContaViewDto getContaContrapartida() {
        return contaContrapartida;
    }

    public void setContaContrapartida(ContaViewDto contaContrapartida) {
        this.contaContrapartida = contaContrapartida;
    }

    public SentidoOperacao getSentido() {
        return sentido;
    }

    public void setSentido(SentidoOperacao sentido) {
        this.sentido = sentido;
    }

    public BigDecimal getValorMatematico() {
        return valorMatematico;
    }

    public void setValorMatematico(BigDecimal valorMatematico) {
        this.valorMatematico = valorMatematico;
    }
}
