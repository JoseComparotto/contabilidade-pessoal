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
import java.util.stream.Stream;

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

        List<MovimentoDto> movimentos = coletarMovimentosDaConta(idConta, efetivo);
        Map<LocalDate, List<MovimentoDto>> movimentosPorData = agruparPorData(movimentos);
        
        return calcularSaldosAcumuladosEAgrupar(movimentosPorData, conta, efetivo);
    }

    private List<MovimentoDto> coletarMovimentosDaConta(Integer idConta, boolean efetivo) {
        List<Lancamento> lancamentosCredito = lancamentoRepository.findByContaCreditoId(idConta);
        List<Lancamento> lancamentosDebito = lancamentoRepository.findByContaDebitoId(idConta);

        return Stream.concat(
                lancamentosCredito.stream().map(LancamentoMapper::toMovimentoCredito),
                lancamentosDebito.stream().map(LancamentoMapper::toMovimentoDebito)
        )
        .filter(m -> efetivo ? StatusLancamento.EFETIVO.equals(m.getStatus())
                : StatusLancamento.PREVISTO.equals(m.getStatus()))
        .sorted(Comparator
                .comparing(MovimentoDto::getData)
                .thenComparing(this::compararPorNaturaEId))
        .collect(Collectors.toList());
    }

    private Map<LocalDate, List<MovimentoDto>> agruparPorData(List<MovimentoDto> movimentos) {
        return movimentos.stream()
                .collect(Collectors.groupingBy(MovimentoDto::getData, HashMap::new, Collectors.toList()));
    }

    private List<MovimentoDto> calcularSaldosAcumuladosEAgrupar(
            Map<LocalDate, List<MovimentoDto>> movimentosPorData,
            ContaViewDto conta,
            boolean efetivo) {
        
        List<MovimentoDto> resultado = new ArrayList<>();
        BigDecimal saldoAcumulado = efetivo ? BigDecimal.ZERO : conta.getSaldoAtual();

        List<LocalDate> datasOrdenadas = movimentosPorData.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        for (LocalDate data : datasOrdenadas) {
            BigDecimal saldoAnterior = saldoAcumulado;
            
            for (MovimentoDto movimento : movimentosPorData.get(data)) {
                saldoAcumulado = saldoAcumulado.add(BigDecimal.valueOf(movimento.getValor()));
                movimento.setSaldo(saldoAcumulado.doubleValue());
                resultado.add(movimento);
            }

            resultado.add(criarMovimentoAgregado(data, saldoAnterior, saldoAcumulado));
        }

        ordenarResultadoFinal(resultado, efetivo);
        return resultado;
    }

    private MovimentoDto criarMovimentoAgregado(LocalDate data, BigDecimal saldoAnterior, BigDecimal saldoAcumulado) {
        MovimentoDto agregado = new MovimentoDto();
        BigDecimal totalDia = saldoAcumulado.subtract(saldoAnterior);
        
        agregado.setAgregado(true);
        agregado.setData(data);
        agregado.setValor(totalDia.doubleValue());
        agregado.setSaldo(saldoAcumulado.doubleValue());
        agregado.setSentidoNatural(totalDia.signum() >= 0 ? SentidoNatural.ENTRADA : SentidoNatural.SAIDA);
        
        return agregado;
    }

    private void ordenarResultadoFinal(List<MovimentoDto> movimentos, boolean efetivo) {
        Comparator<MovimentoDto> comparador = Comparator
                .comparing(MovimentoDto::getData, efetivo ? Comparator.reverseOrder() : Comparator.naturalOrder())
                .thenComparing(MovimentoDto::isAgregado, Comparator.reverseOrder())
                .thenComparing(efetivo ? this::compararPorNaturaEIdReversed : this::compararPorNaturaEId);
        
        movimentos.sort(comparador);
    }

    private int compararPorNaturaEId(MovimentoDto m1, MovimentoDto m2) {
        // Lançamentos com valor positivo (ENTRADA) vêm antes dos negativos (SAIDA)
        boolean isPositive1 = m1.getValor() >= 0;
        boolean isPositive2 = m2.getValor() >= 0;
        
        if (isPositive1 != isPositive2) {
            return Boolean.compare(isPositive2, isPositive1); // positivos primeiro
        }
        
        // Se mesma natureza, ordenar por ID do lançamento
        return m1.getIdLancamento().compareTo(m2.getIdLancamento());
    }
    private int compararPorNaturaEIdReversed(MovimentoDto m1, MovimentoDto m2) {
        // Lançamentos com valor positivo (ENTRADA) vêm antes dos negativos (SAIDA)
        boolean isPositive1 = m1.getValor() >= 0;
        boolean isPositive2 = m2.getValor() >= 0;
        
        if (isPositive1 != isPositive2) {
            return Boolean.compare(isPositive1, isPositive2); // negativos primeiro
        }
        
        // Se mesma natureza, ordenar por ID do lançamento
        return m2.getIdLancamento().compareTo(m1.getIdLancamento());
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
