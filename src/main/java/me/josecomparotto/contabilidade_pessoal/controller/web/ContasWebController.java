package me.josecomparotto.contabilidade_pessoal.controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
}
