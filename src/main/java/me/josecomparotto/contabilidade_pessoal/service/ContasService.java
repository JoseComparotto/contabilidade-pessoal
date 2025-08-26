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
import me.josecomparotto.contabilidade_pessoal.model.dto.ContaFlatDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.ContaTreeDto;
import me.josecomparotto.contabilidade_pessoal.model.entity.Conta;
import me.josecomparotto.contabilidade_pessoal.repository.ContaRepository;

@Service
public class ContasService {

    @Autowired
    private ContaRepository contaRepository;

    public List<Conta> listarContas() {
        return contaRepository.findAll();
    }

    public List<?> listarContasPorView(String view) {
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

        Map<Integer, ContaTreeDto> dtoMap = new HashMap<>();
        Map<Integer, Integer> seqMap = new HashMap<>();
        for (Conta c : all) {
            dtoMap.put(c.getId(), ContaMapper.toTreeDto(c));
            seqMap.put(c.getId(), c.getSequencia());
        }

        List<ContaTreeDto> roots = new ArrayList<>();
        for (Conta c : all) {
            ContaTreeDto dto = dtoMap.get(c.getId());
            if (c.getSuperior() != null) {
                ContaTreeDto parentDto = dtoMap.get(c.getSuperior().getId());
                if (parentDto != null) {
                    parentDto.getInferiores().add(dto);
                } else {
                    roots.add(dto);
                }
            } else {
                roots.add(dto);
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

    public Object obterContaPorIdPorView(Integer id, String view) {
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
}
