package com.gerador.dietas.controller;

import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.dto.ProfileRequest;
import com.gerador.dietas.dto.ProfileResponse;
import com.gerador.dietas.security.AppUserPrincipal;
import com.gerador.dietas.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal AppUserPrincipal principal) {
        Profile profile = profileService.getByUserId(principal.getId());
        return ProfileResponse.from(profile);
    }

    @PutMapping
    public ProfileResponse upsert(@AuthenticationPrincipal AppUserPrincipal principal,
                                  @Valid @RequestBody ProfileRequest request) {
        Profile profile = profileService.upsert(principal.getId(), request);
        return ProfileResponse.from(profile);
    }
}
