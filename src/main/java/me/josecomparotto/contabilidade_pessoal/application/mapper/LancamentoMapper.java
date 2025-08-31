package me.josecomparotto.contabilidade_pessoal.application.mapper;

import java.util.List;
import java.util.stream.Collectors;

import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaDto;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoOperacao;

public class LancamentoMapper {

    public static LancamentoDto toDto(Lancamento lancamento) {
        if (lancamento == null) {
            return null;
        }

        LancamentoDto dto = new LancamentoDto();
        dto.setId(lancamento.getId());
        dto.setDescricao(lancamento.getDescricao());
        dto.setValor(lancamento.getValor());
        dto.setDataCompetencia(lancamento.getDataCompetencia());
        dto.setContaCredito(ContaMapper.toViewDto(lancamento.getContaCredito()));
        dto.setContaDebito(ContaMapper.toViewDto(lancamento.getContaDebito()));

        return dto;
    }

    public static Lancamento fromDto(LancamentoDto dto) {
        if (dto == null) {
            return null;
        }

        Lancamento lancamento = new Lancamento();
        lancamento.setId(dto.getId());
        lancamento.setDescricao(dto.getDescricao());
        lancamento.setValor(dto.getValor());
        lancamento.setDataCompetencia(dto.getDataCompetencia());

        return lancamento;
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
        if (l == null) return null;
        LancamentoPartidaDto dto = new LancamentoPartidaDto();
        dto.setId(l.getId());
        dto.setDescricao(l.getDescricao());
        dto.setDataCompetencia(l.getDataCompetencia());
        dto.setContaPartida(ContaMapper.toViewDto(l.getContaDebito()));
        dto.setContaContrapartida(ContaMapper.toViewDto(l.getContaCredito()));
        dto.setSentido(SentidoOperacao.DEBITO);
        dto.setValorMatematico(l.getValor() == null ? null : l.getValor().negate());
        return dto;
    }

    public static LancamentoPartidaDto toPartidaCredito(Lancamento l) {
        if (l == null) return null;
        LancamentoPartidaDto dto = new LancamentoPartidaDto();
        dto.setId(l.getId());
        dto.setDescricao(l.getDescricao());
        dto.setDataCompetencia(l.getDataCompetencia());
        dto.setContaPartida(ContaMapper.toViewDto(l.getContaCredito()));
        dto.setContaContrapartida(ContaMapper.toViewDto(l.getContaDebito()));
        dto.setSentido(SentidoOperacao.CREDITO);
        dto.setValorMatematico(l.getValor());
        return dto;
    }

    public static List<LancamentoPartidaDto> toPartidas(Lancamento l) {
        return List.of(toPartidaDebito(l), toPartidaCredito(l));
    }

}
