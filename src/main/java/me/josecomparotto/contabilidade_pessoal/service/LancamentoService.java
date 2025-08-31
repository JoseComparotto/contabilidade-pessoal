package me.josecomparotto.contabilidade_pessoal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.josecomparotto.contabilidade_pessoal.application.mapper.LancamentoMapper;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.repository.LancamentoRepository;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;

@Service
public class LancamentoService {

    @Autowired
    private LancamentoRepository lancamentoRepository;

    public List<LancamentoDto> listarLancamentos() {
        return LancamentoMapper.toDtoList(lancamentoRepository.findAll());
    }

    public LancamentoDto obterLancamentoPorId(Long id) {
        return LancamentoMapper.toDto(lancamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado")));
    }

    public LancamentoPartidaDto obterLancamentoPartidaPorId(Long id, SentidoContabil sentidoContabil) {
        return lancamentoRepository.findById(id)
                .map(l -> sentidoContabil == SentidoContabil.DEBITO
                        ? LancamentoMapper.toPartidaDebito(l)
                        : LancamentoMapper.toPartidaCredito(l))
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado"));
    }

    public List<LancamentoPartidaDto> listarLancamentosPorConta(Integer id) {

        List<Lancamento> lancamentosCredito = lancamentoRepository.findByContaCreditoId(id);
        List<Lancamento> lancamentosDebito = lancamentoRepository.findByContaDebitoId(id);

        List<LancamentoPartidaDto> partidas = new ArrayList<>();

        partidas.addAll(lancamentosCredito.stream()
                .map(LancamentoMapper::toPartidaCredito)
                .collect(Collectors.toList()));

        partidas.addAll(lancamentosDebito.stream()
                .map(LancamentoMapper::toPartidaDebito)
                .collect(Collectors.toList()));

        partidas.sort((l1, l2) -> l2.getDataCompetencia().compareTo(l1.getDataCompetencia())); // mais recentes

        return partidas;

    }

}
