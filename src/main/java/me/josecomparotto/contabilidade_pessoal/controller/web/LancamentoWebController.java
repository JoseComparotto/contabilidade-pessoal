package me.josecomparotto.contabilidade_pessoal.controller.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil;
import me.josecomparotto.contabilidade_pessoal.model.enums.SentidoNatural;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaEditDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaNewDto;
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

    // GET /lancamentos/new?contaPartidaId={contaId}
    @GetMapping("/lancamentos/new")
    public String novoLancamento(
            @RequestParam(required = false) Integer contaPartidaId,
            Model model) {

        LancamentoPartidaNewDto novoLancamento = new LancamentoPartidaNewDto();

        novoLancamento.setContaPartidaId(contaPartidaId);

        List<ContaViewDto> contas = lancamentoService.obterContasDisponiveis();
        List<SentidoNatural> sentidosNaturais = List.of(SentidoNatural.values());
        
        model.addAttribute("mode", "create");
        model.addAttribute("lancamento", novoLancamento);
        model.addAttribute("contas", contas);
        model.addAttribute("sentidosNaturais", sentidosNaturais);
        return "lancamentos/form";
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

    // POST /lancamentos
    @PostMapping("/lancamentos")
    public String criarLancamento(
            @ModelAttribute LancamentoPartidaNewDto lancamentoDto,
            RedirectAttributes redirectAttrs) {
        try {
            lancamentoService.criarLancamento(lancamentoDto);
            redirectAttrs.addFlashAttribute("success", "Lançamento criado com sucesso.");
            return "redirect:/contas/" + lancamentoDto.getContaPartidaId();
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/lancamentos/new?contaPartidaId=" + lancamentoDto.getContaPartidaId();
        }
    }
    
    // GET /lancamentos/{id}/edit?sentidoContabil=DEBITO|CREDITO
    @GetMapping("/lancamentos/{id}/edit")
    public String editarLancamento(@PathVariable Long id, Model model, RedirectAttributes redirectAttrs,
            @RequestParam(required = false, defaultValue = "CREDITO") SentidoContabil sentidoContabil) {
        model.addAttribute("sentidoContabil", sentidoContabil);
        LancamentoPartidaDto lancamento = lancamentoService.obterLancamentoPartidaPorId(id, sentidoContabil);
        if (lancamento == null) {
            redirectAttrs.addFlashAttribute("error", "Lançamento não encontrado.");
            return "redirect:/lancamentos";
        }
        model.addAttribute("mode", "edit");
        model.addAttribute("lancamento", lancamento);
        model.addAttribute("contas", lancamentoService.obterContasDisponiveis());
        model.addAttribute("sentidosNaturais", List.of(SentidoNatural.values()));
        return "lancamentos/form";
    }

    
    // POST /lancamentos/{id}/edit
    @PostMapping("/lancamentos/{id}/edit")
    public String salvarEdicao(@PathVariable Long id, LancamentoPartidaEditDto lancamentoDto, RedirectAttributes redirectAttrs) {
        try {
            lancamentoService.atualizarLancamento(id, lancamentoDto);
            redirectAttrs.addFlashAttribute("success", "Lançamento atualizado com sucesso.");
            return "redirect:/lancamentos/" + id;
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/lancamentos/" + id + "/edit";
        }
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
