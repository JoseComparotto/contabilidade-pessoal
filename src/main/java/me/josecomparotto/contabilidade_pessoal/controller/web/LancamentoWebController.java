package me.josecomparotto.contabilidade_pessoal.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoOperacao;
import me.josecomparotto.contabilidade_pessoal.service.LancamentoService;

@Controller
public class LancamentoWebController {

    @Autowired
    private LancamentoService lancamentoService;

    // GET /lancamentos?sentido=DEBITO|CREDITO (default: CREDITO)
    @GetMapping("/lancamentos")
    public String listarLancamentos(
            Model model,
            @RequestParam(name = "sentido", required = false, defaultValue = "CREDITO") SentidoOperacao sentido) {
        model.addAttribute("sentido", sentido);
        model.addAttribute("lancamentos", lancamentoService.listarLancamentosPartidas(sentido));
        return "lancamentos/list";
    }

    // GET /lancamentos/{id}?sentido=DEBITO|CREDITO
    @GetMapping("/lancamentos/{id}")
    public String detalhesLancamento(
            @PathVariable Long id,
            @RequestParam(name = "sentido", required = false, defaultValue = "CREDITO") SentidoOperacao sentido,
            Model model) {
        model.addAttribute("sentido", sentido);
        model.addAttribute("lancamento", lancamentoService.obterLancamentoPartidaPorId(id, sentido));
        return "lancamentos/detail";
    }
}
