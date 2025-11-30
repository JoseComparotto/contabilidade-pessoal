package me.josecomparotto.contabilidade_pessoal.service;

import java.beans.PropertyDescriptor;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Service;

import me.josecomparotto.contabilidade_pessoal.application.mapper.ContaMapper;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaEditDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaNewDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;
import me.josecomparotto.contabilidade_pessoal.repository.ContaRepository;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;

@Service
public class ContaService {

    @Autowired
    private ContaRepository contaRepository;

    public List<ContaViewDto> listarContas() {
        List<Conta> all = contaRepository.findAllWithSuperior();
        List<ContaViewDto> list = ContaMapper.toViewList(all);

        // Map para navegar a cadeia de superiores sem novas consultas
        Map<Integer, Conta> byId = new HashMap<>();
        for (Conta c : all) {
            byId.put(c.getId(), c);
        }

        // Preencher o campo 'path' de cada conta
        Map<Integer, List<Integer>> pathMap = new HashMap<>();
        for (Conta c : all) {
            pathMap.put(c.getId(), c.getPath());
        }

        // Ordenar lexicograficamente por caminho de sequencias (ex.: [1], [1,1], [1,2],
        // [2])
        list.sort((a, b) -> comparePaths(
                pathMap.get(a.getId()),
                pathMap.get(b.getId())));
        return list;
    }

    public List<ContaViewDto> listarContasSinteticas() {
        return listarContas().stream()
                .filter(c -> c.getTipo() == TipoConta.SINTETICA)
                .toList();
    }

    public List<ContaViewDto> listarContasAnaliticas() {
        return listarContas().stream()
                .filter(c -> c.getTipo() == TipoConta.ANALITICA)
                .toList();
    }

    public List<ContaViewDto> listarContasAnaliticasPorSentidoAceito(SentidoContabil aceitaSentido) {

        return listarContas().stream()
                .filter(c -> c.getTipo() == TipoConta.ANALITICA)
                .filter(c -> c.getAceitaSentido(aceitaSentido))
                .toList();
    }

    public ContaViewDto obterContaPorId(Integer id) {
        Optional<Conta> opt = contaRepository.findByIdWithSuperior(id);
        if (opt.isEmpty())
            return null;
        Conta c = opt.get();
        ContaViewDto dto = ContaMapper.toViewDto(c);
        return dto;
    }

    // Compara dois caminhos lexicograficamente
    private int comparePaths(List<Integer> a, List<Integer> b) {
        if (a == b)
            return 0;
        if (a == null)
            return -1;
        if (b == null)
            return 1;
        int size = Math.min(a.size(), b.size());
        for (int i = 0; i < size; i++) {
            int cmp = Integer.compare(a.get(i), b.get(i));
            if (cmp != 0)
                return cmp;
        }
        return Integer.compare(a.size(), b.size());
    }

    public boolean deletarContaPorId(Integer id) {
        Optional<Conta> opt = contaRepository.findById(id);
        if (opt.isEmpty())
            return false;
        Conta c = opt.get();

        if (!c.isDeletable()) {
            throw new IllegalStateException("Conta não pode ser deletada");
        }

        contaRepository.delete(c);
        return true;
    }

    public ContaViewDto criarConta(ContaNewDto contaDto) {

        // Validar dados iniciais da conta
        if (contaDto.getDescricao() == null || contaDto.getDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da conta é obrigatória");
        }
        if (contaDto.getTipo() == null) {
            throw new IllegalArgumentException("Tipo da conta é obrigatório");
        }

        // Mapear DTO para entidade
        Conta conta = ContaMapper.fromNewDto(contaDto);
        if (conta == null) {
            throw new IllegalArgumentException("Dados da conta inválidos");
        }

        // Se houver superiorId, buscar a conta superior
        if (contaDto.getSuperiorId() != null) {
            Optional<Conta> optSup = contaRepository.findById(contaDto.getSuperiorId());
            if (optSup.isEmpty()) {
                throw new IllegalArgumentException("Conta superior não encontrada: " + contaDto.getSuperiorId());
            }
            Conta sup = optSup.get();
            conta.setSuperior(sup);

            // Se a conta superior for redutora, a nova conta também deve ser redutora
            if (sup.isRedutora()) {
                contaDto.setRedutora(true);
            }

            // Definir se a conta é redutora
            conta.setRedutora(contaDto.isRedutora());

            // Definir sequencia como o próximo número disponível entre os inferiores
            int nextSeq = 1;
            for (Conta inf : sup.getInferiores()) {
                if (inf.getSequencia() >= nextSeq) {
                    nextSeq = inf.getSequencia() + 1;
                }
            }
            conta.setSequencia(nextSeq);
        } else {
            // Lança exceção se não houver conta superior
            throw new IllegalArgumentException("Conta superior não informada");
        }

        // Salvar para gerar ID e código
        conta = contaRepository.save(conta);

        return ContaMapper.toViewDto(conta);
    }

    public ContaViewDto atualizarConta(Integer id, ContaEditDto contaDto) {
        Optional<Conta> opt = contaRepository.findById(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Conta não encontrada: " + id);
        }
        Conta conta = opt.get();
        List<Conta> inferiores = conta.getInferiores();

        // Verificar se a conta pode ser editada
        if (!conta.isEditable()) {
            throw new IllegalStateException("Conta não pode ser editada");
        }

        // Verificar se os campos que foram alterados são editáveis (via propriedades
        // Spring/JavaBean)
        {
            Set<String> editaveis = conta.getEditableProperties();
            BeanWrapper dtoBw = new BeanWrapperImpl(contaDto);
            BeanWrapper entBw = new BeanWrapperImpl(conta);

            for (PropertyDescriptor pd : dtoBw.getPropertyDescriptors()) {
                String prop = pd.getName();
                if ("class".equals(prop))
                    continue;

                if (!dtoBw.isReadableProperty(prop) || !entBw.isReadableProperty(prop)) {
                    continue; // ignora propriedades sem getter correspondente
                }

                Object novo = dtoBw.getPropertyValue(prop);
                Object atual = entBw.getPropertyValue(prop);

                boolean alterado = !Objects.equals(novo, atual);
                if (alterado && !editaveis.contains(prop)) {
                    throw new IllegalStateException("Campo '" + prop + "' não é editável para esta conta");
                }
            }
        }

        // Validar
        if (contaDto.getDescricao() == null || contaDto.getDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da conta é obrigatória");
        }
        if (contaDto.getTipo() == null) {
            throw new IllegalArgumentException("Tipo da conta é obrigatório");
        }
        if (contaDto.isRedutora() && !inferiores.stream().allMatch(Conta::isRedutora)) {
            throw new IllegalArgumentException(
                    "Conta não pode ser redutora se possuir contas inferiores não redutoras");
        }

        // Atualizar descrição e tipo
        conta.setDescricao(contaDto.getDescricao());
        conta.setTipo(contaDto.getTipo());
        conta.setRedutora(contaDto.isRedutora());

        // Salvar alterações
        conta = contaRepository.save(conta);
        return ContaMapper.toViewDto(conta);
    }

    public List<ContaViewDto> listarInferioresPorConta(Integer id) {
        Optional<Conta> opt = contaRepository.findById(id);
        if (opt.isEmpty()) {
            return List.of();
        }
        Conta conta = opt.get();
        if (conta.getInferiores() == null) {
            return List.of();
        }

        return conta.getInferiores().stream()
                .sorted(Comparator.comparing(Conta::getSequencia))
                .map(ContaMapper::toViewDto)
                .collect(Collectors.toList());
    }

    public ContaViewDto obterSuperiorPorConta(Integer id) {
        Optional<Conta> opt = contaRepository.findById(id);
        if (opt.isEmpty()) {
            return null;
        }
        Conta c = opt.get();
        Conta sup = c.getSuperior();
        if (sup == null) return null;
        
        return ContaMapper.toViewDto(sup);
    }

}
