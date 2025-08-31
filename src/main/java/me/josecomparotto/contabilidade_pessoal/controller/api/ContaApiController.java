package me.josecomparotto.contabilidade_pessoal.controller.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaEditDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaViewDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaNewDto;
import me.josecomparotto.contabilidade_pessoal.service.ContaService;

@RestController
@RequestMapping("/api/contas")
public class ContaApiController {

    @Autowired
    private ContaService contasService;

    // GET /api/contas
    @GetMapping
    public List<?> listarContas() {
        return contasService.listarContas();
    }

    // GET /api/contas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> obterConta(@PathVariable Integer id) {
        Object body = contasService.obterContaPorId(id);
        if (body == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(body);
    }

    // POST /api/contas
    @PostMapping
    public ResponseEntity<?> criarConta(@RequestBody ContaNewDto contaDto) {
        try {
            ContaViewDto novaConta = contasService.criarConta(contaDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaConta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/contas/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarConta(@PathVariable Integer id, @RequestBody ContaEditDto contaDto) {
        try {
            ContaViewDto contaAtualizada = contasService.atualizarConta(id, contaDto);
            return ResponseEntity.ok(contaAtualizada);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.toLowerCase().contains("não encontrada")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(msg);
            }
            return ResponseEntity.badRequest().body(msg);
        }
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
