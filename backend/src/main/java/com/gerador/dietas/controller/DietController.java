package com.gerador.dietas.controller;

import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.dto.DietPlanResponse;
import com.gerador.dietas.security.AppUserPrincipal;
import com.gerador.dietas.service.DietService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @Operation(summary = "Lista o histórico de dietas do usuário autenticado")
    @GetMapping
    public List<DietPlanResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return dietService.listForUser(principal.getId()).stream()
                .map(DietPlanResponse::from)
                .toList();
    }

    @Operation(summary = "Detalhe de uma dieta específica do usuário autenticado")
    @GetMapping("/{id}")
    public DietPlanResponse get(@AuthenticationPrincipal AppUserPrincipal principal,
                                @PathVariable Long id) {
        DietPlan plan = dietService.getOwned(principal.getId(), id);
        return DietPlanResponse.from(plan);
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
