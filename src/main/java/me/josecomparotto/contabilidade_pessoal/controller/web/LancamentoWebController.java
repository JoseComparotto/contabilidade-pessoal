package me.josecomparotto.contabilidade_pessoal.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;
import me.josecomparotto.contabilidade_pessoal.service.LancamentoService;

@Controller
public class LancamentoWebController {

    @Autowired
    private LancamentoService lancamentoService;

    // GET /lancamentos?sentidoContabil=DEBITO|CREDITO (default: CREDITO)
    @GetMapping("/lancamentos")
    public String listarLancamentos(
            Model model,
            @RequestParam(name = "sentidoContabil", required = false, defaultValue = "CREDITO") SentidoContabil sentidoContabil) {
        model.addAttribute("sentidoContabil", sentidoContabil);
        model.addAttribute("lancamentos", lancamentoService.listarLancamentosPartidas(sentidoContabil));
        return "lancamentos/list";
    }

    // GET /lancamentos/{id}?sentidoContabil=DEBITO|CREDITO
    @GetMapping("/lancamentos/{id}")
    public String detalhesLancamento(
            @PathVariable Long id,
            @RequestParam(name = "sentidoContabil", required = false, defaultValue = "CREDITO") SentidoContabil sentidoContabil,
            Model model) {
        model.addAttribute("sentidoContabil", sentidoContabil);
        model.addAttribute("lancamento", lancamentoService.obterLancamentoPartidaPorId(id, sentidoContabil));
        return "lancamentos/detail";
    }
}
