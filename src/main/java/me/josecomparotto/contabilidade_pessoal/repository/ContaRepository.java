package me.josecomparotto.contabilidade_pessoal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import me.josecomparotto.contabilidade_pessoal.model.Conta;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Integer> {

	@Query("select c from Conta c left join fetch c.superior")
	List<Conta> findAllWithSuperior();

	@Query("select c from Conta c left join fetch c.superior where c.id = :id")
	Optional<Conta> findByIdWithSuperior(@Param("id") Integer id);
}
