package me.josecomparotto.contabilidade_pessoal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import me.josecomparotto.contabilidade_pessoal.model.entity.Lancamento;

@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    List<Lancamento> findByContaCreditoId(Integer contaCreditoId);

    List<Lancamento> findByContaDebitoId(Integer contaDebitoId);

}
