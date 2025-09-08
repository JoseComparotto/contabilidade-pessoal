package me.josecomparotto.contabilidade_pessoal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.josecomparotto.contabilidade_pessoal.application.mapper.LancamentoMapper;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.repository.ContaRepository;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.repository.LancamentoRepository;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaNewDto;
import me.josecomparotto.contabilidade_pessoal.model.enums.Natureza;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoNatural;
import static me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta.*;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;
import static me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil.*;

@Service
public class LancamentoService {

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private ContaRepository contaRepository;

    @Autowired
    private ContaService contaService;

    public List<LancamentoDto> listarLancamentos() {
        return LancamentoMapper.toDtoList(lancamentoRepository.findAll());
    }

    public LancamentoDto obterLancamentoPorId(Long id) {
        return LancamentoMapper.toDto(lancamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado")));
    }

    public LancamentoPartidaDto obterLancamentoPartidaPorId(Long id, SentidoContabil sentidoContabil) {
        return lancamentoRepository.findById(id)
                .map(l -> sentidoContabil == DEBITO
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

    public boolean deletarLancamento(Long id) {
        Optional<Lancamento> opt = lancamentoRepository.findById(id);
        if (opt.isEmpty())
            return false;
        Lancamento l = opt.get();

        if (!l.isDeletable()) {
            throw new IllegalStateException("Lançamento não pode ser deletado");
        }

        lancamentoRepository.delete(l);
        return true;
    }

    public List<ContaViewDto> obterContasDisponiveis() {
        return contaService.listarContasAnaliticas().stream()
                .filter(ContaViewDto::isAtiva) // Filtra apenas contas ativas
                .collect(Collectors.toList());
    }

    public Long criarLancamento(LancamentoPartidaNewDto dto) {
        if (dto == null)
            throw new IllegalArgumentException("DTO não pode ser nulo");
        if (dto.getDataCompetencia() == null)
            throw new IllegalArgumentException("Data de competência obrigatória");
        if (dto.getContaPartidaId() == null)
            throw new IllegalArgumentException("Conta partida obrigatória");
        if (dto.getContaContrapartidaId() == null)
            throw new IllegalArgumentException("Conta contrapartida obrigatória");
        if (dto.getContaPartidaId().equals(dto.getContaContrapartidaId()))
            throw new IllegalArgumentException("Conta partida e contrapartida devem ser diferentes");
        if (dto.getValorAbsoluto() == null || BigDecimal.ZERO.equals(dto.getValorAbsoluto()))
            throw new IllegalArgumentException("Valor deve ser diferente de zero");

        var contaPartida = contaRepository.findById(dto.getContaPartidaId())
                .orElseThrow(() -> new IllegalArgumentException("Conta partida não encontrada"));
        var contaContrapartida = contaRepository.findById(dto.getContaContrapartidaId())
                .orElseThrow(() -> new IllegalArgumentException("Conta contrapartida não encontrada"));

        if (contaPartida.getTipo() != ANALITICA || contaContrapartida.getTipo() != ANALITICA) {
            throw new IllegalArgumentException("Apenas contas analíticas podem receber lançamentos");
        }
        if (!contaPartida.isAtiva() || !contaContrapartida.isAtiva()) {
            throw new IllegalArgumentException("Contas inativas não podem receber lançamentos");
        }

        // Determinar sentido contábil a partir do sentido natural informado em relação
        // à conta partida
        // Regra inversa da usada no mapper: se conta partida é credora, entrada =
        // crédito; se devedora, entrada = débito.
        boolean partidaEhCredora = contaPartida.getNatureza() == Natureza.CREDORA;
        boolean entrada = dto.getSentidoNatural() == SentidoNatural.ENTRADA;

        boolean usaCreditoComoPartida = partidaEhCredora ? entrada : !entrada; // true => partida é crédito; false =>
                                                                               // partida é débito

        SentidoContabil sentidoPartida = usaCreditoComoPartida ? CREDITO : DEBITO;
        SentidoContabil sentidoContrapartida = usaCreditoComoPartida ? DEBITO : CREDITO;

        if (!contaPartida.isAceitaSentido(sentidoPartida)) {
            throw new IllegalArgumentException("Conta partida não aceita lançamentos neste sentido");
        }
        if (!contaContrapartida.isAceitaSentido(sentidoContrapartida)) {
            throw new IllegalArgumentException("Conta contrapartida não aceita lançamentos neste sentido");
        }

        var lanc = new Lancamento();
        lanc.setDescricao(dto.getDescricao());
        lanc.setDataCompetencia(dto.getDataCompetencia());
        lanc.setValor(dto.getValorAbsoluto());

        if (usaCreditoComoPartida) {
            lanc.setContaCredito(contaPartida);
            lanc.setContaDebito(contaContrapartida);
        } else {
            lanc.setContaDebito(contaPartida);
            lanc.setContaCredito(contaContrapartida);
        }

        var saved = lancamentoRepository.save(lanc);
        return saved.getId();
    }

}
