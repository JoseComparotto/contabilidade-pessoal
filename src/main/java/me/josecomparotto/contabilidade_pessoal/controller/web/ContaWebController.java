package me.josecomparotto.contabilidade_pessoal.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.RequestParam;

import me.josecomparotto.contabilidade_pessoal.application.mapper.ContaMapper;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaEditDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaNewDto;
import me.josecomparotto.contabilidade_pessoal.model.enums.TipoConta;
import me.josecomparotto.contabilidade_pessoal.service.ContaService;

@Controller
public class ContaWebController {

    @Autowired
    private ContaService contasService;

    // GET /contas
    @GetMapping("/contas")
    public String listarContas(Model model) {
        model.addAttribute("contas", contasService.listarContas());
        return "contas/list";
    }

    // GET /contas/{id}
    @GetMapping("/contas/{id}")
    public String detalhesConta(@PathVariable Integer id, Model model, RedirectAttributes redirectAttrs) {
        ContaViewDto dto = contasService.obterContaPorId(id);
        if (dto == null) {
            redirectAttrs.addFlashAttribute("error", "Conta não encontrada.");
            return "redirect:/contas";
        }
        model.addAttribute("conta", dto);
        return "contas/detail";
    }

    // GET /contas/new
    @GetMapping("/contas/new")
    public String novaConta(Model model,
            @RequestParam(name = "superiorId", required = false) Integer superiorId) {
        model.addAttribute("mode", "create");
        ContaNewDto nova = new ContaNewDto();
        if (superiorId != null) {
            nova.setSuperiorId(superiorId);
        }
        model.addAttribute("conta", nova);
        model.addAttribute("tipos", TipoConta.values());
        model.addAttribute("contas", contasService.listarContasSinteticas());
        return "contas/form";
    }

    // POST /contas[?redirect={redirectUrl}] (create)
    @PostMapping("/contas")
    public String criarConta(ContaNewDto contaDto, RedirectAttributes redirectAttrs,
                              @RequestParam(name = "redirect", required = false) String redirectUrl) {
        try {
            ContaViewDto criada = contasService.criarConta(contaDto);
            redirectAttrs.addFlashAttribute("success", "Conta criada com sucesso: " + criada.getCodigo());
            return "redirect:" + sanitizeRedirect(redirectUrl);
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/contas/new";
        }
    }

    private String sanitizeRedirect(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) return "/contas";
        // Only allow relative paths within the app to avoid open redirects
        if (redirectUrl.startsWith("/") && !redirectUrl.startsWith("//")) {
            return redirectUrl;
        }
        return "/contas";
    }

    // GET /contas/{id}/edit
    @GetMapping("/contas/{id}/edit")
    public String editarConta(@PathVariable Integer id, Model model, RedirectAttributes redirectAttrs) {
        ContaViewDto dto = contasService.obterContaPorId(id);
        if (dto == null) {
            redirectAttrs.addFlashAttribute("error", "Conta não encontrada.");
            return "redirect:/contas";
        }
        model.addAttribute("mode", "edit");
        model.addAttribute("conta", dto);
        model.addAttribute("tipos", TipoConta.values());
        model.addAttribute("contas", contasService.listarContasSinteticas());
        return "contas/form";
    }

    // POST /contas/{id}/edit
    @PostMapping("/contas/{id}/edit")
    public String salvarEdicao(@PathVariable Integer id, ContaViewDto contaDto, RedirectAttributes redirectAttrs) {
        try {
            ContaEditDto editDto = ContaMapper.toEditDto(contaDto);
            contasService.atualizarConta(id, editDto);
            redirectAttrs.addFlashAttribute("success", "Conta atualizada com sucesso.");
            return "redirect:/contas/" + id;
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
            return "redirect:/contas/" + id + "/edit";
        }
    }

    // POST /contas/{id}/delete
    @PostMapping("/contas/{id}/delete")
    public String deletarConta(@PathVariable Integer id, RedirectAttributes redirectAttrs) {
        try {
            boolean removido = contasService.deletarContaPorId(id);
            if (!removido) {
                redirectAttrs.addFlashAttribute("error", "Conta não encontrada.");
            } else {
                redirectAttrs.addFlashAttribute("success", "Conta excluída com sucesso.");
            }
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/contas";
    }
}
