package me.josecomparotto.contabilidade_pessoal.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.josecomparotto.contabilidade_pessoal.application.mapper.LancamentoMapper;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoEditDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoNewDto;
import me.josecomparotto.contabilidade_pessoal.repository.ContaRepository;
import me.josecomparotto.contabilidade_pessoal.repository.LancamentoRepository;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.MovimentoDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoNatural;
import me.josecomparotto.contabilidade_pessoal.model.enums.StatusLancamento;

import static me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil.CREDITO;
import static me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil.DEBITO;
import static me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta.*;

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

    public List<MovimentoDto> listarMovimentosPorConta(Integer idConta, boolean efetivo) {
        ContaViewDto conta = contaService.obterContaPorId(idConta);
        if (conta == null) {
            throw new IllegalArgumentException("Conta não encontrada");
        }

        Map<LocalDate, List<MovimentoDto>> dataLancamentosMap = new HashMap<>();
        List<MovimentoDto> movimentos = new ArrayList<>();

        List<Lancamento> lancamentosCredito = lancamentoRepository.findByContaCreditoId(idConta);
        List<Lancamento> lancamentosDebito = lancamentoRepository.findByContaDebitoId(idConta);

        movimentos.addAll(lancamentosCredito.stream()
                .map(LancamentoMapper::toMovimentoCredito)
                .collect(Collectors.toList()));
        movimentos.addAll(lancamentosDebito.stream()
                .map(LancamentoMapper::toMovimentoDebito)
                .collect(Collectors.toList()));

        movimentos = movimentos.stream()
                .filter(m -> efetivo ? StatusLancamento.EFETIVO.equals(m.getStatus())
                        : StatusLancamento.PREVISTO.equals(m.getStatus()))
                .sorted(Comparator
                        .comparing(MovimentoDto::getData)
                        .thenComparing(MovimentoDto::getValor, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        for (MovimentoDto m : movimentos) {
            LocalDate data = m.getData();
            dataLancamentosMap.computeIfAbsent(data, k -> new ArrayList<>()).add(m);
        }

        movimentos.clear();

        List<LocalDate> datasOrdenadas = new ArrayList<>(dataLancamentosMap.keySet());
        datasOrdenadas.sort(Comparator.naturalOrder());

        BigDecimal saldoAcumulado = efetivo ? BigDecimal.ZERO : conta.getSaldoAtual();
        for (var data : datasOrdenadas) {
            var mLancamentos = dataLancamentosMap.get(data);

            BigDecimal saldoAnterior = saldoAcumulado;

            for (var m : mLancamentos) {
                saldoAcumulado = saldoAcumulado.add(BigDecimal.valueOf(m.getValor()));
                m.setSaldo(saldoAcumulado.doubleValue());
                movimentos.add(m);
            }

            MovimentoDto mAgregado = new MovimentoDto();
            mAgregado.setAgregado(true);
            mAgregado.setData(data);
            mAgregado.setValor(saldoAcumulado.subtract(saldoAnterior).doubleValue());
            mAgregado.setSaldo(saldoAcumulado.doubleValue());
            mAgregado.setSentidoNatural(mAgregado.getValor() >= 0 ? SentidoNatural.ENTRADA : SentidoNatural.SAIDA);
            movimentos.add(mAgregado);
        }

        if (efetivo) {
            movimentos.sort(Comparator
                    .comparing(MovimentoDto::getData, Comparator.reverseOrder())
                    .thenComparing(MovimentoDto::isAgregado, Comparator.reverseOrder())
                    .thenComparing(MovimentoDto::getValor));
        } else {
            movimentos.sort(Comparator
                    .comparing(MovimentoDto::getData)
                    .thenComparing(MovimentoDto::isAgregado, Comparator.reverseOrder())
                    .thenComparing(MovimentoDto::getValor, Comparator.reverseOrder()));
        }
        return movimentos;
    }

    public boolean deletarLancamentoPorId(Long id) {
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

    public LancamentoDto criarLancamento(LancamentoNewDto dto) {

        // Validações iniciais
        if (dto == null)
            throw new IllegalArgumentException("DTO não pode ser nulo");
        if (dto.getDataCompetencia() == null)
            throw new IllegalArgumentException("Data de competência obrigatória");
        if (dto.getContaCreditoId() == null)
            throw new IllegalArgumentException("Conta de origem (crédito) obrigatória");
        if (dto.getContaDebitoId() == null)
            throw new IllegalArgumentException("Conta de destino (débito) obrigatória");
        if (dto.getContaDebitoId().equals(dto.getContaCreditoId()))
            throw new IllegalArgumentException("Conta de origem (crédito) e de destino (débito) devem ser diferentes");
        if (dto.getValor() == null || dto.getValor() <= 0.0)
            throw new IllegalArgumentException("Valor deve ser positivo e diferente de zero");

        Conta contaCredito = contaRepository.findById(dto.getContaCreditoId())
                .orElse(null);
        Conta contaDebito = contaRepository.findById(dto.getContaDebitoId())
                .orElse(null);

        // Validações das contas
        if (contaCredito == null)
            throw new IllegalArgumentException("Conta de origem (crédito) não encontrada");
        if (contaDebito == null)
            throw new IllegalArgumentException("Conta de destino (débito) não encontrada");
        if (!contaCredito.isAtiva() || !contaDebito.isAtiva())
            throw new IllegalArgumentException("Contas inativas não podem receber lançamentos");
        if (contaCredito.getTipo() != ANALITICA || contaDebito.getTipo() != ANALITICA)
            throw new IllegalArgumentException("Apenas contas analíticas podem receber lançamentos");
        if (!contaCredito.getAceitaSentido(CREDITO))
            throw new IllegalArgumentException("Conta de origem (crédito) não aceita lançamentos neste sentido");
        if (!contaDebito.getAceitaSentido(DEBITO))
            throw new IllegalArgumentException("Conta de destino (débito) não aceita lançamentos neste sentido");

        // Mapear DTO para entidade
        Lancamento lancamento = LancamentoMapper.fromNewDto(dto, contaCredito, contaDebito);
        if (lancamento == null) {
            throw new IllegalArgumentException("Erro ao mapear DTO para entidade");
        }

        // Salvar entidade
        lancamentoRepository.save(lancamento);
        return LancamentoMapper.toDto(lancamento);
    }

    public void atualizarLancamento(Long id, LancamentoEditDto lancamentoDto) {
        if (lancamentoDto == null)
            throw new IllegalArgumentException("DTO não pode ser nulo");
        if (lancamentoDto.getDataCompetencia() == null)
            throw new IllegalArgumentException("Data de competência obrigatória");
        if (lancamentoDto.getContaCreditoId() == null)
            throw new IllegalArgumentException("Conta de origem (crédito) obrigatória");
        if (lancamentoDto.getContaDebitoId() == null)
            throw new IllegalArgumentException("Conta de destino (débito) obrigatória");
        if (lancamentoDto.getContaDebitoId().equals(lancamentoDto.getContaCreditoId()))
            throw new IllegalArgumentException("Conta de origem (crédito) e de destino (débito) devem ser diferentes");
        if (lancamentoDto.getValor() == null || lancamentoDto.getValor() <= 0.0)
            throw new IllegalArgumentException("Valor deve ser positivo e diferente de zero");

        Lancamento lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lançamento não encontrado"));

        if (!lancamento.isEditable()) {
            throw new IllegalStateException("Lançamento não pode ser editado");
        }

        Conta contaCredito = contaRepository.findById(lancamentoDto.getContaCreditoId())
                .orElse(null);
        Conta contaDebito = contaRepository.findById(lancamentoDto.getContaDebitoId())
                .orElse(null);

        // Validações das contas
        if (contaCredito == null)
            throw new IllegalArgumentException("Conta de origem (crédito) não encontrada");
        if (contaDebito == null)
            throw new IllegalArgumentException("Conta de destino (débito) não encontrada");
        if (!contaCredito.isAtiva() || !contaDebito.isAtiva())
            throw new IllegalArgumentException("Contas inativas não podem receber lançamentos");
        if (contaCredito.getTipo() != ANALITICA || contaDebito.getTipo() != ANALITICA)
            throw new IllegalArgumentException("Apenas contas analíticas podem receber lançamentos");
        if (!contaCredito.getAceitaSentido(CREDITO))
            throw new IllegalArgumentException("Conta de origem (crédito) não aceita lançamentos neste sentido");
        if (!contaDebito.getAceitaSentido(DEBITO))
            throw new IllegalArgumentException("Conta de destino (débito) não aceita lançamentos neste sentido");

        lancamento.setDescricao(lancamentoDto.getDescricao());
        lancamento.setDataCompetencia(lancamentoDto.getDataCompetencia());
        lancamento.setValor(BigDecimal.valueOf(lancamentoDto.getValor()));
        lancamento.setContaCredito(contaCredito);
        lancamento.setContaDebito(contaDebito);
        lancamento.setStatus(lancamentoDto.getStatus());

        lancamentoRepository.save(lancamento);
    }

}
