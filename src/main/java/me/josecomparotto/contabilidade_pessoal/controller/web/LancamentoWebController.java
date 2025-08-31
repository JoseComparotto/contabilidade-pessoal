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

    // GET /lancamentos/{id}?sentidoContabil=DEBITO|CREDITO
    @GetMapping("/lancamentos/{id}")
    public String detalhesLancamento(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "CREDITO") SentidoContabil sentidoContabil,
            Model model) {
        model.addAttribute("sentidoContabil", sentidoContabil);
        model.addAttribute("lancamento", lancamentoService.obterLancamentoPartidaPorId(id, sentidoContabil));
        return "lancamentos/detail";
    }
}
