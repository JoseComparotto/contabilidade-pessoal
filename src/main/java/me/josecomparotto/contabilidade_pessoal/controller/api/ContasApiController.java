package me.josecomparotto.contabilidade_pessoal.controller.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import me.josecomparotto.contabilidade_pessoal.service.ContasService;

@RestController
@RequestMapping("/api/contas")
public class ContasApiController {

    @Autowired
    private ContasService contasService;

    // GET /api/contas?view=tree|flat (default=flat)
    @GetMapping
    public List<?> listarContas(@RequestParam(defaultValue = "flat") String view) {
        return contasService.listarContasPorView(view);
    }

    // GET /api/contas/{id}?view=tree|flat (default=flat)
    @GetMapping("/{id}")
    public ResponseEntity<?> obterConta(@PathVariable Integer id,
            @RequestParam(defaultValue = "flat") String view) {
        Object body = contasService.obterContaPorIdPorView(id, view);
        if (body == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(body);
    }

    // DELETE /api/contas/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletarConta(@PathVariable Integer id) {
        try {
            boolean removido = contasService.deletarContaPorId(id);
            if (!removido)
                return ResponseEntity.notFound().build();
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
