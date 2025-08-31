package me.josecomparotto.contabilidade_pessoal.controller.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.josecomparotto.contabilidade_pessoal.model.dto.lancamento.LancamentoDto;
import me.josecomparotto.contabilidade_pessoal.service.LancamentoService;

@RestController
@RequestMapping("/api/lancamentos")
public class LancamentoApiController {

    @Autowired
    private LancamentoService lancamentoService;

    public LancamentoApiController(LancamentoService lancamentoService) {
        this.lancamentoService = lancamentoService;
    }

    // GET /api/lancamentos
    @GetMapping
    public CollectionModel<EntityModel<LancamentoDto>> listarLancamentos() {
        List<EntityModel<LancamentoDto>> items = lancamentoService.listarLancamentos().stream()
                .map(this::toModel)
                .collect(Collectors.toList());

        Link self = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(LancamentoApiController.class)
                .listarLancamentos()).withSelfRel();
        return CollectionModel.of(items, self);
    }

    // GET /api/lancamentos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<LancamentoDto>> obterLancamento(@PathVariable Long id) {
        try {
            LancamentoDto lancamento = lancamentoService.obterLancamentoPorId(id);
            return ResponseEntity.ok(toModel(lancamento));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private EntityModel<LancamentoDto> toModel(LancamentoDto dto) {
        EntityModel<LancamentoDto> model = EntityModel.of(dto);
        Link self = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(LancamentoApiController.class)
                .obterLancamento(dto.getId())).withSelfRel();
        Link collection = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(LancamentoApiController.class)
                .listarLancamentos()).withRel("collection");
        model.add(self, collection);
        return model;
    }
}
