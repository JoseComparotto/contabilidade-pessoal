package me.josecomparotto.contabilidade_pessoal.model.dto.lancamento;

import java.time.LocalDate;
import java.text.NumberFormat;
import java.util.Locale;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.hateoas.server.core.Relation;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoNatural;

@Relation(collectionRelation = "lancamentos", itemRelation = "lancamento")
public class LancamentoPartidaDto implements IDto<Lancamento> {

    private Long id;
    private String descricao;
    private LocalDate dataCompetencia;

    private int contaPartidaId;
    private int contaContrapartidaId;

    private ContaViewDto contaPartida;
    private ContaViewDto contaContrapartida;

    private SentidoContabil sentidoContabil;
    private SentidoNatural sentidoNatural;

    private Double valorContabil; // negativo quando sentidoContabil = DEBITO
    private Double valorNatural; // negativo quando o sentidoContabil for contra a natureza da conta de partida
    private Double valorAbsoluto; // sempre positivo

    private boolean editable;
    private boolean deletable;

    private String displayText;

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

    public int getContaPartidaId() {
        return contaPartidaId;
    }

    public void setContaPartidaId(int contaPartidaId) {
        this.contaPartidaId = contaPartidaId;
    }

    public int getContaContrapartidaId() {
        return contaContrapartidaId;
    }

    public void setContaContrapartidaId(int contaContrapartidaId) {
        this.contaContrapartidaId = contaContrapartidaId;
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

    public Double getValorContabil() {
        return valorContabil;
    }

    public void setValorContabil(Double valorContabil) {
        this.valorContabil = valorContabil;
    }

    public Double getValorNatural() {
        return valorNatural;
    }

    public void setValorNatural(Double valorNatural) {
        this.valorNatural = valorNatural;
    }

    public Double getValorAbsoluto() {
        return valorAbsoluto;
    }

    public void setValorAbsoluto(Double valor) {
        this.valorAbsoluto = valor;
    }

    @JsonIgnore
    public String getValorNaturalFormatado() {
        if (valorNatural == null || Double.compare(valorNatural, 0.0) == 0) {
            return "R$ 0,00";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
        return nf.format(valorNatural);
    }

    @JsonIgnore
    public String getDataCompetenciaFormatada() {
        if (dataCompetencia == null)
            return "—";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dataCompetencia.format(fmt);
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    @Override
    public String toString() {
        return getDisplayText();
    }
}
