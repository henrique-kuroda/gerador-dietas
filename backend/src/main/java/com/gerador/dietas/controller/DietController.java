package com.gerador.dietas.controller;

import com.gerador.dietas.domain.DietPlan;
import com.gerador.dietas.dto.DietPlanResponse;
import com.gerador.dietas.security.AppUserPrincipal;
import com.gerador.dietas.service.DietService;
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

    @PostMapping("/generate")
    public DietPlanResponse generate(@AuthenticationPrincipal AppUserPrincipal principal) {
        DietPlan plan = dietService.generate(principal.getId());
        return DietPlanResponse.from(plan);
    }

    @GetMapping
    public List<DietPlanResponse> list(@AuthenticationPrincipal AppUserPrincipal principal) {
        return dietService.listForUser(principal.getId()).stream()
                .map(DietPlanResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DietPlanResponse get(@AuthenticationPrincipal AppUserPrincipal principal,
                                @PathVariable Long id) {
        DietPlan plan = dietService.getOwned(principal.getId(), id);
        return DietPlanResponse.from(plan);
    }
}
