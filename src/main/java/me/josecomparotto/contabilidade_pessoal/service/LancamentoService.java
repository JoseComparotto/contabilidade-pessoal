package me.josecomparotto.contabilidade_pessoal.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.josecomparotto.contabilidade_pessoal.application.mapper.ContaMapper;
import me.josecomparotto.contabilidade_pessoal.application.mapper.LancamentoMapper;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.repository.ContaRepository;
import me.josecomparotto.contabilidade_pessoal.repository.LancamentoRepository;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;

@Service
public class LancamentoService {

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private ContaRepository contaRepository;

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
        Optional<Conta> opt = contaRepository.findById(id);
        if (opt.isEmpty()) {
            return List.of();
        }
        Conta c = opt.get();
        ContaViewDto contaViewDto = ContaMapper.toViewDto(c);
        return contaViewDto.getLancamentos();
    }

}
