package me.josecomparotto.contabilidade_pessoal.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

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

    // POST /lancamentos/{id}/delete[?redirect={redirectUrl}]
    @PostMapping("/lancamentos/{id}/delete")
    public String deletarLancamento(@PathVariable Long id, RedirectAttributes redirectAttrs,
            @RequestParam(name = "redirect", required = false, defaultValue = "/contas") String redirectUrl) {
        try {
            boolean removido = lancamentoService.deletarLancamento(id);
            if (!removido) {
                redirectAttrs.addFlashAttribute("error", "Lançamento não encontrado.");
            } else {
                redirectAttrs.addFlashAttribute("success", "Lançamento excluído com sucesso.");
            }
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + sanitizeRedirect(redirectUrl);
    }

    private String sanitizeRedirect(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank())
            return "/contas";
        // Only allow relative paths within the app to avoid open redirects
        if (redirectUrl.startsWith("/") && !redirectUrl.startsWith("//")) {
            return redirectUrl;
        }
        return "/contas";
    }
}
