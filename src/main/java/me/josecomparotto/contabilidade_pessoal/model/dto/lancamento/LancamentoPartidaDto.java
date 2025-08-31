package me.josecomparotto.contabilidade_pessoal.model.dto.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.text.NumberFormat;
import java.util.Locale;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoNatural;

public class LancamentoPartidaDto implements IDto<Lancamento> {

    private Long id;
    private String descricao;
    private LocalDate dataCompetencia;

    private ContaViewDto contaPartida;
    private ContaViewDto contaContrapartida;

    private SentidoContabil sentidoContabil;
    private SentidoNatural sentidoNatural;
    
    private BigDecimal valorContabil; // negativo quando sentidoContabil = DEBITO
    private BigDecimal valorNatural; // negativo quando o sentidoContabil for contra a natureza da conta de partida

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

    public SentidoContabil getSentidoContabil() {
        return sentidoContabil;
    }

    public void setSentidoContabil(SentidoContabil sentidoContabil) {
        this.sentidoContabil = sentidoContabil;
    }

    public SentidoNatural getSentidoNatural() {
        return sentidoNatural;
    }

    public void setSentidoNatural(SentidoNatural sentidoNatural) {
        this.sentidoNatural = sentidoNatural;
    }

    public BigDecimal getValorContabil() {
        return valorContabil;
    }

    public void setValorContabil(BigDecimal valorContabil) {
        this.valorContabil = valorContabil;
    }

    public BigDecimal getValorNatural() {
        return valorNatural;
    }

    public void setValorNatural(BigDecimal valorNatural) {
        this.valorNatural = valorNatural;
    }

    @JsonIgnore
    public String getValorNaturalFormatado() {
        if (valorNatural == null || BigDecimal.ZERO.compareTo(valorNatural) == 0) {
            return "R$ 0,00";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return nf.format(valorNatural);
    }

    @JsonIgnore
    public String getDataCompetenciaFormatada() {
        if (dataCompetencia == null) return "—";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dataCompetencia.format(fmt);
    }

}
