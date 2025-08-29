package me.josecomparotto.contabilidade_pessoal.application.mapper;

import java.util.List;
import java.util.stream.Collectors;

import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaEditDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaFlatDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaNewDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaTreeDto;
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
        dto.setDisplayText(conta.getDisplayText());
        dto.setNatureza(conta.getNatureza());
        dto.setTipo(conta.getTipo());
        dto.setSuperiorId(conta.getSuperior() != null ? conta.getSuperior().getId() : null);
        dto.setPath(conta.getPath());
        dto.setSaldoAtual(conta.getSaldoNatural());
        dto.setTipoMovimento(conta.getTipoMovimento());
        dto.setTiposMovimentoPossiveis(conta.getTiposMovimentoPossiveis());
        dto.setEditable(conta.isEditable());
        dto.setDeletable(conta.isDeletable());
        dto.setEditableProperties(conta.getEditableProperties());
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
        dto.setDisplayText(conta.getDisplayText());
        dto.setNatureza(conta.getNatureza());
        dto.setTipo(conta.getTipo());
        dto.setSaldoAtual(conta.getSaldoNatural());
        dto.setTipoMovimento(conta.getTipoMovimento());
        dto.setTiposMovimentoPossiveis(conta.getTiposMovimentoPossiveis());
        dto.setEditable(conta.isEditable());
        dto.setDeletable(conta.isDeletable());
        dto.setEditableProperties(conta.getEditableProperties());
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

    public static Conta fromNewDto(ContaNewDto dto) {
        if (dto == null)
            return null;
        Conta conta = new Conta();
        conta.setDescricao(dto.getDescricao());
        conta.setTipo(dto.getTipo());
        conta.setCreatedBySystem(false);
        return conta;
    }

    public static ContaEditDto toEditDto(ContaFlatDto contaOld) {
        if (contaOld == null)
            return null;
        ContaEditDto dto = new ContaEditDto();
        dto.setDescricao(contaOld.getDescricao());
        dto.setTipo(contaOld.getTipo());
        dto.setTipoMovimento(contaOld.getTipoMovimento());
        return dto;
    }
}
