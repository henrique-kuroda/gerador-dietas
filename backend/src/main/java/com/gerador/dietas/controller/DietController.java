package com.gerador.dietas.controller;

import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.dto.DietPlanResponse;
import com.gerador.dietas.dto.DietPlanSummaryResponse;
import com.gerador.dietas.security.AppUserPrincipal;
import com.gerador.dietas.service.DietService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diet")
public class DietController {

    private final DietService dietService;

    public DietController(DietService dietService) {
        this.dietService = dietService;
    }

    @Operation(summary = "Calcula TMB/TDEE e gera uma nova dieta via LLM para o usuário autenticado")
    @PostMapping("/generate")
    public DietPlanResponse generate(@AuthenticationPrincipal AppUserPrincipal principal) {
        DietPlan plan = dietService.generate(principal.getId());
        return DietPlanResponse.from(plan);
    }

    @Operation(summary = "Lista paginada do histórico de dietas (resumo, sem o cardápio completo)")
    @GetMapping
    public Page<DietPlanSummaryResponse> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return dietService.listForUser(principal.getId(), pageable)
                .map(DietPlanSummaryResponse::from);
    }

    @Operation(summary = "Detalhe de uma dieta específica do usuário autenticado")
    @GetMapping("/{id}")
    public DietPlanResponse get(@AuthenticationPrincipal AppUserPrincipal principal,
                                @PathVariable Long id) {
        DietPlan plan = dietService.getOwned(principal.getId(), id);
        return DietPlanResponse.from(plan);
    }

    @Operation(summary = "Remove uma dieta do histórico do usuário autenticado")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal principal,
                       @PathVariable Long id) {
        dietService.delete(principal.getId(), id);
    }

    @Operation(summary = "Exporta uma dieta em PDF (com perfil, métricas calculadas e plano alimentar)")
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@AuthenticationPrincipal AppUserPrincipal principal,
                                            @PathVariable Long id) {
        byte[] pdf = dietService.renderPdf(principal.getId(), id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "dieta-" + id + ".pdf");
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, 200);
    }
}
