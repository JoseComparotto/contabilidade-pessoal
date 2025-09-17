package me.josecomparotto.contabilidade_pessoal.application.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaDto;

import static me.josecomparotto.contabilidade_pessoal.model.enums.Natureza.*;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;
import static me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil.*;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoNatural;
import static me.josecomparotto.contabilidade_pessoal.model.enums.SentidoNatural.*;

public class LancamentoMapper {

    public static LancamentoDto toDto(Lancamento lancamento) {
        if (lancamento == null) {
            return null;
        }

        LancamentoDto dto = new LancamentoDto();
        dto.setId(lancamento.getId());
        dto.setDescricao(lancamento.getDescricao());
        dto.setValor(lancamento.getValor().doubleValue());
        dto.setDataCompetencia(lancamento.getDataCompetencia());
        dto.setContaCredito(ContaMapper.toViewDto(lancamento.getContaCredito()));
        dto.setContaDebito(ContaMapper.toViewDto(lancamento.getContaDebito()));
        dto.setEditable(lancamento.isEditable());
        dto.setDeletable(lancamento.isDeletable());
        dto.setDisplayText(lancamento.getDisplayText());

        return dto;
    }

    public static List<LancamentoDto> toDtoList(List<Lancamento> all) {
        if (all == null) {
            return null;
        }

        return all.stream()
                .map(LancamentoMapper::toDto)
                .collect(Collectors.toList());
    }

    public static LancamentoPartidaDto toPartidaDebito(Lancamento l) {
        return toPartida(l, DEBITO);
    }

    public static LancamentoPartidaDto toPartidaCredito(Lancamento l) {
        return toPartida(l, CREDITO);
    }

    public static LancamentoPartidaDto toPartida(Lancamento l, SentidoContabil sentidoContabil) {
        if (l == null)
            return null;

        BigDecimal valorAbsoluto = l.getValor();
        var contaPartida = sentidoContabil == DEBITO ? l.getContaDebito() : l.getContaCredito();
        var contaContrapartida = sentidoContabil == DEBITO ? l.getContaCredito() : l.getContaDebito();

        if (valorAbsoluto == null || BigDecimal.ZERO.equals(valorAbsoluto))
            throw new IllegalStateException("Valor do lancamento não pode ser nulo");
        if(contaPartida == null || contaContrapartida == null)
            throw new IllegalStateException("Conta partida e contrapartida não podem ser nulas");

        boolean creditoEhEntrada = contaPartida.getNatureza() == CREDORA;

        boolean ehEntrada = creditoEhEntrada
                ? sentidoContabil == CREDITO
                : sentidoContabil == DEBITO;

        SentidoNatural sentidoNatural = ehEntrada ? ENTRADA : SAIDA;

        BigDecimal valorContabil = valorAbsoluto;
        BigDecimal valorNatural = ehEntrada ? valorAbsoluto : valorAbsoluto.negate();

        LancamentoPartidaDto dto = new LancamentoPartidaDto();
        dto.setId(l.getId());
        dto.setDescricao(l.getDescricao());
        dto.setDataCompetencia(l.getDataCompetencia());
        dto.setContaPartidaId(contaPartida.getId());
        dto.setContaContrapartidaId(contaContrapartida.getId());
        dto.setContaPartida(ContaMapper.toViewDto(contaPartida));
        dto.setContaContrapartida(ContaMapper.toViewDto(contaContrapartida));
        dto.setSentidoContabil(sentidoContabil);
        dto.setSentidoNatural(sentidoNatural);
        dto.setValorContabil(valorContabil.doubleValue());
        dto.setValorNatural(valorNatural.doubleValue());
        dto.setValorAbsoluto(valorAbsoluto.doubleValue());
        dto.setStatus(l.getStatus());
        dto.setEditable(l.isEditable());
        dto.setDeletable(l.isDeletable());
        dto.setDisplayText(l.getDisplayText());
        return dto;
    }
}
