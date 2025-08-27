package me.josecomparotto.contabilidade_pessoal.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.josecomparotto.contabilidade_pessoal.service.ContasService;

@Controller
public class ContasWebController {

    @Autowired
    private ContasService contasService;

    // GET /contas
    @GetMapping("/contas")
    public String listarContas(Model model) {
        model.addAttribute("contas", contasService.listarContasFlat());
        return "contas/list";
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
