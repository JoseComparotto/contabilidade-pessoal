package me.josecomparotto.contabilidade_pessoal.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.josecomparotto.contabilidade_pessoal.application.mapper.LancamentoMapper;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.repository.LancamentoRepository;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaDto;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoOperacao;

@Service
public class LancamentoService {

    @Autowired
    private final LancamentoRepository lancamentoRepository;

    public LancamentoService(LancamentoRepository lancamentoRepository) {
        this.lancamentoRepository = lancamentoRepository;
    }

    public List<LancamentoDto> listarLancamentos() {
        return LancamentoMapper.toDtoList(lancamentoRepository.findAll());
    }

    public List<LancamentoPartidaDto> listarLancamentosPartidas(SentidoOperacao sentido) {
    return lancamentoRepository.findAll().stream()
        .map(l -> sentido == SentidoOperacao.DEBITO
            ? LancamentoMapper.toPartidaDebito(l)
            : LancamentoMapper.toPartidaCredito(l))
        .toList();
    }

    public LancamentoDto obterLancamentoPorId(Long id) {
        return LancamentoMapper.toDto(lancamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado")));
    }

    public LancamentoPartidaDto obterLancamentoPartidaPorId(Long id, SentidoOperacao sentido) {
    return lancamentoRepository.findById(id)
        .map(l -> sentido == SentidoOperacao.DEBITO
            ? LancamentoMapper.toPartidaDebito(l)
            : LancamentoMapper.toPartidaCredito(l))
        .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado"));
    }
}
