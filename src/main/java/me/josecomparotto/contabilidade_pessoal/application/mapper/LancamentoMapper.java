package me.josecomparotto.contabilidade_pessoal.application.mapper;

import java.util.List;
import java.util.stream.Collectors;

import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;

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
        dto.setContaCredito(ContaMapper.toFlatDto(lancamento.getContaCredito()));
        dto.setContaDebito(ContaMapper.toFlatDto(lancamento.getContaDebito()));

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

}
