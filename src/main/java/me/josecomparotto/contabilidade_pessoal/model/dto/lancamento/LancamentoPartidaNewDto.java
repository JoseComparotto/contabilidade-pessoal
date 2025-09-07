package me.josecomparotto.contabilidade_pessoal.model.dto.lancamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.hateoas.server.core.Relation;

import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoNatural;

@Relation(collectionRelation = "lancamentos", itemRelation = "lancamento")
public class LancamentoPartidaNewDto implements IDto<Lancamento> {

    private String descricao;
    private LocalDate dataCompetencia;

    private Integer contaPartidaId;
    private Integer contaContrapartidaId;

    private SentidoNatural sentidoNatural;
    
    private BigDecimal valorAbsoluto;

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

    public Integer getContaPartidaId() {
        return contaPartidaId;
    }

    public void setContaPartidaId(Integer contaPartidaId) {
        this.contaPartidaId = contaPartidaId;
    }

    public Integer getContaContrapartidaId() {
        return contaContrapartidaId;
    }

    public void setContaContrapartidaId(Integer contaContrapartidaId) {
        this.contaContrapartidaId = contaContrapartidaId;
    }

    public SentidoNatural getSentidoNatural() {
        return sentidoNatural;
    }

    public void setSentidoNatural(SentidoNatural sentidoNatural) {
        this.sentidoNatural = sentidoNatural;
    }

    public BigDecimal getValorAbsoluto() {
        return valorAbsoluto;
    }

    public void setValorAbsoluto(BigDecimal valorNatural) {
        this.valorAbsoluto = valorNatural;
    }

    
}
