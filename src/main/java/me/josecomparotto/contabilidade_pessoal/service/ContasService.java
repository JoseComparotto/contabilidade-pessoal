package me.josecomparotto.contabilidade_pessoal.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import me.josecomparotto.contabilidade_pessoal.application.mapper.ContaMapper;
import me.josecomparotto.contabilidade_pessoal.model.dto.ContaEditDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.ContaFlatDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.ContaNewDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.ContaTreeDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.IDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;
import me.josecomparotto.contabilidade_pessoal.repository.ContaRepository;

@Service
public class ContasService {

    @Autowired
    private ContaRepository contaRepository;

    public List<Conta> listarContas() {
        return contaRepository.findAll();
    }

    public List<? extends IDto<Conta>> listarContasPorView(String view) {
        if (view == null || "flat".equalsIgnoreCase(view)) {
            return listarContasFlat();
        } else if ("tree".equalsIgnoreCase(view)) {
            return listarContasTree();
        } else {
            throw new IllegalArgumentException("Visualização não suportada: " + view);
        }
    }

    public List<ContaTreeDto> listarContasTree() {
        List<Conta> all = contaRepository.findAllWithSuperior();

        Map<Integer, Integer> seqMap = new HashMap<>();
        for (Conta c : all) {
            seqMap.put(c.getId(), c.getSequencia());
        }

        List<ContaTreeDto> roots = new ArrayList<>();
        for (Conta c : all) {
            if (c.getSuperior() == null) {
                roots.add(ContaMapper.toTreeDto(c));
            }
        }

        // Ordenar raízes e filhos por sequencia
        ordenarArvore(roots, seqMap);
        return roots;
    }

    public List<ContaFlatDto> listarContasFlat() {
        List<Conta> all = contaRepository.findAllWithSuperior();
        List<ContaFlatDto> list = ContaMapper.toFlatList(all);

        // Map para navegar a cadeia de superiores sem novas consultas
        Map<Integer, Conta> byId = new HashMap<>();
        for (Conta c : all) {
            byId.put(c.getId(), c);
        }

        // Ordenar lexicograficamente por caminho de sequencias (ex.: [1], [1,1], [1,2],
        // [2])
        list.sort((a, b) -> comparePaths(
                a.getPath(),
                b.getPath()));
        return list;
    }

    public List<ContaFlatDto> listarContasSinteticas() {
        return listarContasFlat().stream()
                .filter(c -> c.getTipo() == TipoConta.SINTETICA)
                .toList();
    }

    public IDto<Conta> obterContaPorIdPorView(Integer id, String view) {
        if ("tree".equalsIgnoreCase(view)) {
            return obterContaTree(id);
        }
        return obterContaFlat(id);
    }

    public ContaFlatDto obterContaFlat(Integer id) {
        Optional<Conta> opt = contaRepository.findByIdWithSuperior(id);
        if (opt.isEmpty())
            return null;
        Conta c = opt.get();
        ContaFlatDto dto = ContaMapper.toFlatDto(c);
        return dto;
    }

    public ContaTreeDto obterContaTree(Integer id) {
        // Para uma conta específica em árvore, retornamos o nó com filhos montados.
        // Estratégia simples: carregar todos e montar árvore, então pegar o nó pelo id.
        List<ContaTreeDto> arvore = listarContasTree();
        // indexar por id
        Map<Integer, ContaTreeDto> index = new HashMap<>();
        List<ContaTreeDto> stack = new ArrayList<>(arvore);
        while (!stack.isEmpty()) {
            ContaTreeDto node = stack.remove(stack.size() - 1);
            index.put(node.getId(), node);
            stack.addAll(node.getInferiores());
        }
        return index.get(id);
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

    private void ordenarArvore(List<ContaTreeDto> nodes, Map<Integer, Integer> seqMap) {
        nodes.sort(Comparator
                .comparing((ContaTreeDto n) -> seqMap.getOrDefault(n.getId(), Integer.MAX_VALUE))
                .thenComparing(ContaTreeDto::getId));
        for (ContaTreeDto n : nodes) {
            ordenarArvore(n.getInferiores(), seqMap);
        }
    }

    public boolean deletarContaPorId(Integer id) {
        Optional<Conta> opt = contaRepository.findById(id);
        if (opt.isEmpty()) return false;
        Conta c = opt.get();

        if (!c.isDeletable()) {
            throw new IllegalStateException("Conta não pode ser deletada");
        }

        contaRepository.delete(c);
        return true;
    }

    public ContaFlatDto criarConta(ContaNewDto contaDto) {

        // Validar dados da conta
        if(contaDto.getDescricao() == null || contaDto.getDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da conta é obrigatória");
        }
        if(contaDto.getTipo() == null) {
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

            // Definir natureza da conta
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

        return ContaMapper.toFlatDto(conta);
    }

    public ContaFlatDto atualizarConta(Integer id, ContaEditDto contaDto) {
        Optional<Conta> opt = contaRepository.findById(id);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Conta não encontrada: " + id);
        }
        Conta conta = opt.get();

        if (!conta.isEditable()) {
            throw new IllegalStateException("Conta não pode ser editada");
        }

        // Validar
        if (contaDto.getDescricao() == null || contaDto.getDescricao().trim().isEmpty()) {
            throw new IllegalArgumentException("Descrição da conta é obrigatória");
        }
        if (contaDto.getTipo() == null) {
            throw new IllegalArgumentException("Tipo da conta é obrigatório");
        }

        // Atualizar descrição e tipo
        conta.setDescricao(contaDto.getDescricao());
        conta.setTipo(contaDto.getTipo());
        conta.setRedutora(contaDto.isRedutora());

        // Salvar alterações
        conta = contaRepository.save(conta);
        return ContaMapper.toFlatDto(conta);
    }
}
