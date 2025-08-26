package me.josecomparotto.contabilidade_pessoal.application.mapper;

import java.util.List;
import java.util.stream.Collectors;

import me.josecomparotto.contabilidade_pessoal.model.dto.ContaFlatDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.ContaTreeDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;

public final class ContaMapper {

    private ContaMapper() {
    }

    public static ContaFlatDto toFlatDto(Conta conta) {
        if (conta == null)
            return null;
        ContaFlatDto dto = new ContaFlatDto();
        dto.setId(conta.getId());
        dto.setCodigo(conta.getCodigo());
        dto.setDescricao(conta.getDescricao());
        dto.setNatureza(conta.getNatureza());
        dto.setTipo(conta.getTipo());
        dto.setSuperiorId(conta.getSuperior() != null ? conta.getSuperior().getId() : null);
        dto.setPath(conta.getPath());
        return dto;
    }

    public static List<ContaFlatDto> toFlatList(List<Conta> contas) {
        if (contas == null)
            return null;
        return contas.stream()
                .map(ContaMapper::toFlatDto)
                .collect(Collectors.toList());
    }

    public static ContaTreeDto toTreeDto(Conta conta) {
        if (conta == null)
            return null;
        ContaTreeDto dto = new ContaTreeDto();
        dto.setId(conta.getId());
        dto.setCodigo(conta.getCodigo());
        dto.setDescricao(conta.getDescricao());
        dto.setNatureza(conta.getNatureza());
        dto.setTipo(conta.getTipo());
        if (conta.getInferiores() != null && !conta.getInferiores().isEmpty()) {
            dto.getInferiores().addAll(
                    conta.getInferiores().stream()
                            .map(ContaMapper::toTreeDto)
                            .collect(Collectors.toList()));
        }
        return dto;
    }

    public static List<ContaTreeDto> toTreeList(List<Conta> contas) {
        if (contas == null)
            return null;
        return contas.stream()
                .map(ContaMapper::toTreeDto)
                .collect(Collectors.toList());
    }
}
