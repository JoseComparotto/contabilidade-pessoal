package me.josecomparotto.contabilidade_pessoal.controller.web;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.josecomparotto.contabilidade_pessoal.application.mapper.LancamentoMapper;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoEditDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoNewDto;
import me.josecomparotto.contabilidade_pessoal.model.enums.StatusLancamento;
import me.josecomparotto.contabilidade_pessoal.service.ContaService;
import me.josecomparotto.contabilidade_pessoal.service.LancamentoService;

import static me.josecomparotto.contabilidade_pessoal.model.enums.SentidoContabil.*;
import static me.josecomparotto.contabilidade_pessoal.model.enums.StatusLancamento.*;

import java.util.Arrays;
import java.util.List;

@Controller
public class LancamentoWebController {

    @Autowired
    private ContaService contaService;

    @Autowired
    private LancamentoService lancamentoService;

    // GET /lancamentos/{id}[?redirect={redirectUrl}]
    @GetMapping("/lancamentos/{id}")
    public String detalhesLancamento(
            @PathVariable Long id,
            @RequestParam(name = "redirect", required = false) String redirectUrl,
            Model model, RedirectAttributes redirectAttrs) {
        LancamentoDto lancamento = lancamentoService.obterLancamentoPorId(id);
        if (lancamento == null) {
            redirectAttrs.addFlashAttribute("error", "Lançamento não encontrado.");
            return "redirect:" + sanitizeRedirect(redirectUrl, "/contas");
        }
        model.addAttribute("lancamento", lancamento);
        model.addAttribute("redirectUrl", sanitizeRedirect(redirectUrl, "/contas"));
        return "lancamentos/detail";
    }

    // GET
    // /lancamentos/new[?status={EFETIVO|PREVISTO}&contaDebitoId={contaId}&contaCreditoId={contaId}]
    @GetMapping("/lancamentos/new")
    public String novoLancamento(Model model, @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer contaDebitoId,
            @RequestParam(required = false) Integer contaCreditoId) {
        LancamentoNewDto novo = new LancamentoNewDto();

        List<ContaViewDto> contasCredito = contaService.listarContasAnaliticasPorSentidoAceito(CREDITO);
        List<ContaViewDto> contasDebito = contaService.listarContasAnaliticasPorSentidoAceito(DEBITO);
        List<StatusLancamento> statusList = List.of(EFETIVO, PREVISTO);

        String redirectUrl = "/contas";

        if (contaDebitoId != null) {
            novo.setContaDebitoId(contaDebitoId);
            redirectUrl = "/contas/" + contaDebitoId;
        }
        if (contaCreditoId != null) {
            novo.setContaCreditoId(contaCreditoId);
            redirectUrl = "/contas/" + contaCreditoId;
        }

        if ("PREVISTO".equalsIgnoreCase(status)) {
            novo.setStatus(PREVISTO);
        } else if ("EFETIVO".equalsIgnoreCase(status)) {
            novo.setStatus(EFETIVO);
        }

        model.addAttribute("mode", "create");
        model.addAttribute("redirectUrl", redirectUrl);
        model.addAttribute("lancamento", novo);
        model.addAttribute("contasDebito", contasDebito);
        model.addAttribute("contasCredito", contasCredito);
        model.addAttribute("statusList", statusList);
        return "lancamentos/form";
    }

    // POST /lancamentos[?redirect={redirectUrl}] (create)
    @PostMapping("lancamentos")
    public String create(LancamentoNewDto dto,
            @RequestParam(name = "redirect", required = false) String redirectUrl,
            RedirectAttributes redirectAttrs) {
        try {
            LancamentoDto created = lancamentoService.criarLancamento(dto);
            return "redirect:" + sanitizeRedirect(redirectUrl, "/lancamentos/" + created.getId());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/lancamentos/new";
        }
    }

    // GET /lancamentos/{id}/edit
    @GetMapping("/lancamentos/{id}/edit")
    public String editarLancamento(@PathVariable Long id, Model model, RedirectAttributes redirectAttrs) {
        try {
            LancamentoEditDto lancamento = LancamentoMapper.toEditDto(lancamentoService.obterLancamentoPorId(id));
            if (lancamento == null) {
                redirectAttrs.addFlashAttribute("error", "Lançamento não encontrado.");
                return "redirect:/contas";
            }

            List<ContaViewDto> contasCredito = contaService.listarContasAnaliticasPorSentidoAceito(CREDITO);
            List<ContaViewDto> contasDebito = contaService.listarContasAnaliticasPorSentidoAceito(DEBITO);
            List<StatusLancamento> statusList = Arrays.asList(StatusLancamento.values());

            model.addAttribute("mode", "edit");
            model.addAttribute("lancamento", lancamento);
            model.addAttribute("contasDebito", contasDebito);
            model.addAttribute("contasCredito", contasCredito);
            model.addAttribute("statusList", statusList);
            return "lancamentos/form";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/lancamentos/" + id;
        }
    }

    // POST /lancamentos/{id}/edit
    @PostMapping("/lancamentos/{id}/edit")
    public String salvarEdicao(@PathVariable Long id, LancamentoEditDto lancamentoDto, RedirectAttributes redirectAttrs) {
        try {
            lancamentoService.atualizarLancamento(id, lancamentoDto);
            redirectAttrs.addFlashAttribute("success", "Lançamento atualizado com sucesso.");
            return "redirect:/lancamentos/" + id;
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/lancamentos/" + id + "/edit";
        }
    }

    // POST /lancamentos/{id}/delete[?redirect={redirectUrl}] (delete)
    @PostMapping("/lancamentos/{id}/delete")
    public String deletarLancamento(@PathVariable Long id, RedirectAttributes redirectAttrs,
            @RequestParam(name = "redirect", required = false) String redirectUrl) {
        try {
            boolean removido = lancamentoService.deletarLancamentoPorId(id);
            if (!removido) {
                redirectAttrs.addFlashAttribute("error", "Lançamento não encontrado.");
            } else {
                redirectAttrs.addFlashAttribute("success", "Lançamento excluído com sucesso.");
            }
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", "Erro ao excluir lançamento: " + e.getMessage());
            return "redirect:/lancamentos/" + id;
        }
        return "redirect:" + sanitizeRedirect(redirectUrl, "/contas");
    }

    private String sanitizeRedirect(String redirectUrl, String defaultUrl) {
        if (redirectUrl == null || redirectUrl.isBlank())
            return defaultUrl;
        // Only allow relative paths within the app to avoid open redirects
        if (redirectUrl.startsWith("/") && !redirectUrl.startsWith("//")) {
            return redirectUrl;
        }
        return defaultUrl;
    }
}
