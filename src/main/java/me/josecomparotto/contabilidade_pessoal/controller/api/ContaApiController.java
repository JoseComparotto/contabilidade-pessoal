package me.josecomparotto.contabilidade_pessoal.controller.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;
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
import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoPartidaDto;
import me.josecomparotto.contabilidade_pessoal.model.dto.conta.ContaNewDto;
import me.josecomparotto.contabilidade_pessoal.service.ContaService;
import me.josecomparotto.contabilidade_pessoal.service.LancamentoService;

@RestController
@RequestMapping("/api/contas")
public class ContaApiController {

    @Autowired
    private ContaService contasService;

    @Autowired
    private LancamentoService lancamentoService;

    // GET /api/contas
    @GetMapping
    public CollectionModel<EntityModel<ContaViewDto>> listarContas() {
        List<EntityModel<ContaViewDto>> content = contasService.listarContas().stream()
                .map(this::toModel)
                .collect(Collectors.toList());
        Link self = linkTo(methodOn(ContaApiController.class).listarContas()).withSelfRel();
        return CollectionModel.of(content, self);
    }

    // GET /api/contas/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> obterConta(@PathVariable Integer id) {
        ContaViewDto body = contasService.obterContaPorId(id);
        if (body == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toModel(body));
    }

    // POST /api/contas
    @PostMapping
    public ResponseEntity<?> criarConta(@RequestBody ContaNewDto contaDto) {
        try {
            ContaViewDto novaConta = contasService.criarConta(contaDto);
            EntityModel<ContaViewDto> model = toModel(novaConta);
            return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/contas/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizarConta(@PathVariable Integer id, @RequestBody ContaEditDto contaDto) {
        try {
            ContaViewDto contaAtualizada = contasService.atualizarConta(id, contaDto);
            return ResponseEntity.ok(toModel(contaAtualizada));
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

    // GET /api/contas/{id}/superior
    @GetMapping("/{id}/superior")
    public ResponseEntity<?> obterSuperiorPorConta(@PathVariable Integer id) {
        ContaViewDto superior = contasService.obterSuperiorPorConta(id);
        if (superior == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toModel(superior));
    }

    // GET /api/contas/{id}/inferiores
    @GetMapping("/{id}/inferiores")
    public ResponseEntity<?> listarInferioresPorConta(@PathVariable Integer id) {
        List<ContaViewDto> inferiores = contasService.listarInferioresPorConta(id);
        if (inferiores == null || inferiores.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<EntityModel<ContaViewDto>> items = inferiores.stream().map(this::toModel).toList();
        Link self = linkTo(methodOn(ContaApiController.class).listarInferioresPorConta(id)).withSelfRel();
        return ResponseEntity.ok(CollectionModel.of(items, self));
    }

    // GET /api/contas/{id}/lancamentos
    @GetMapping("/{id}/lancamentos")
    public ResponseEntity<?> listarLancamentosPorConta(@PathVariable Integer id) {
        List<LancamentoPartidaDto> lancamentos = lancamentoService.listarLancamentosPorConta(id);
        if (lancamentos == null || lancamentos.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // Link self to this collection and link to the owning conta
        Link self = linkTo(methodOn(ContaApiController.class).listarLancamentosPorConta(id)).withSelfRel();
        Link conta = linkTo(methodOn(ContaApiController.class).obterConta(id)).withRel("conta");
        return ResponseEntity.ok(CollectionModel.of(lancamentos, self, conta));
    }

    private EntityModel<ContaViewDto> toModel(ContaViewDto dto) {
        Integer id = dto.getId();
        EntityModel<ContaViewDto> model = EntityModel.of(dto);
        model.add(linkTo(methodOn(ContaApiController.class).obterConta(id)).withSelfRel());
        model.add(linkTo(methodOn(ContaApiController.class).listarInferioresPorConta(id)).withRel("inferiores"));
        model.add(linkTo(methodOn(ContaApiController.class).obterSuperiorPorConta(id)).withRel("superior"));
        model.add(linkTo(methodOn(ContaApiController.class).listarLancamentosPorConta(id)).withRel("lancamentos"));
        model.add(linkTo(methodOn(ContaApiController.class).listarContas()).withRel(IanaLinkRelations.COLLECTION));
        return model;
    }

}
