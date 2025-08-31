package me.josecomparotto.contabilidade_pessoal.controller.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.service.LancamentoService;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoApiController {

    @Autowired
    private LancamentoService lancamentoService;

    public LancamentoApiController(LancamentoService lancamentoService) {
        this.lancamentoService = lancamentoService;
    }

    // GET /api/lancamentos
    @GetMapping
    public List<LancamentoDto> listarLancamentos() {
        return lancamentoService.listarLancamentos();
    }

    // GET /api/lancamentos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<LancamentoDto> obterLancamento(@PathVariable Long id) {
        try {
            LancamentoDto lancamento = lancamentoService.obterLancamentoPorId(id);
            return ResponseEntity.ok(lancamento);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
