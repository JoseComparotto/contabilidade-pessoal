package me.josecomparotto.contabilidade_pessoal.application.mapper;

import java.util.List;
import java.util.stream.Collectors;

import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaEditDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaNewDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;

public final class ContaMapper {

    private ContaMapper() {
    }

    public static ContaViewDto toViewDto(Conta conta) {
        if (conta == null)
            return null;
        ContaViewDto dto = new ContaViewDto();
        dto.setId(conta.getId());
        dto.setCodigo(conta.getCodigo());
        dto.setDescricao(conta.getDescricao());
        dto.setDisplayText(conta.getDisplayText());
        dto.setNatureza(conta.getNatureza());
        dto.setTipo(conta.getTipo());
        dto.setSaldoAtual(conta.getSaldoNatural());
        dto.setRedutora(conta.isRedutora());
        dto.setAceitaMovimentoOposto(conta.getAceitaMovimentoOposto());
        dto.setEditable(conta.isEditable());
        dto.setDeletable(conta.isDeletable());
        dto.setEditableProperties(conta.getEditableProperties());
        return dto;
    }

    public static List<ContaViewDto> toViewList(List<Conta> contas) {
        if (contas == null)
            return null;
        return contas.stream()
                .map(ContaMapper::toViewDto)
                .collect(Collectors.toList());
    }

    public static Conta fromNewDto(ContaNewDto dto) {
        if (dto == null)
            return null;
        Conta conta = new Conta();
        conta.setDescricao(dto.getDescricao());
        conta.setTipo(dto.getTipo());
        conta.setCreatedBySystem(false);
        conta.setAtiva(true); // Nova conta é ativa por padrão
        return conta;
    }

    public static ContaEditDto toEditDto(ContaViewDto contaOld) {
        if (contaOld == null)
            return null;
        ContaEditDto dto = new ContaEditDto();
        dto.setDescricao(contaOld.getDescricao());
        dto.setTipo(contaOld.getTipo());
        dto.setRedutora(contaOld.isRedutora());
        return dto;
    }
}
