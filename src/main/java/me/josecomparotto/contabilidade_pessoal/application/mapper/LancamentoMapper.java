package me.josecomparotto.contabilidade_pessoal.application.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoNewDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.MovimentoDto;

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
        dto.setStatus(lancamento.getStatus());
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

    public static MovimentoDto toMovimentoDebito(Lancamento l) {
        return toMovimento(l, DEBITO);
    }

    public static MovimentoDto toMovimentoCredito(Lancamento l) {
        return toMovimento(l, CREDITO);
    }

    public static MovimentoDto toMovimento(Lancamento l, SentidoContabil sentidoContabil) {
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

        BigDecimal valorNatural = ehEntrada ? valorAbsoluto : valorAbsoluto.negate();

        MovimentoDto dto = new MovimentoDto();
        dto.setIdLancamento(l.getId());
        dto.setDescricao(l.getDescricao());
        dto.setData(l.getDataCompetencia());
        dto.setContaPartida(ContaMapper.toViewDto(contaPartida));
        dto.setContaContrapartida(ContaMapper.toViewDto(contaContrapartida));
        dto.setSentidoNatural(sentidoNatural);
        dto.setValor(valorNatural.doubleValue());
        dto.setStatus(l.getStatus());
        return dto;
    }

    public static Lancamento fromNewDto(LancamentoNewDto dto, Conta contaCredito, Conta contaDebito) {
        if (dto == null) {
            return null;
        }

        Lancamento lancamento = new Lancamento();
        lancamento.setId(dto.getId());
        lancamento.setDescricao(dto.getDescricao());
        lancamento.setValor(dto.getValor() != null ? BigDecimal.valueOf(dto.getValor()) : null);
        lancamento.setDataCompetencia(dto.getDataCompetencia());
        lancamento.setContaCredito(contaCredito);
        lancamento.setContaDebito(contaDebito);
        lancamento.setStatus(dto.getStatus());

        return lancamento;
    }
}
