package com.gerador.dietas.service;

import com.gerador.dietas.domain.Profile;
import com.gerador.dietas.domain.User;
import com.gerador.dietas.dto.ProfileRequest;
import com.gerador.dietas.exception.ProfileNotFoundException;
import com.gerador.dietas.repository.ProfileRepository;
import com.gerador.dietas.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileService(ProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Profile getByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ProfileNotFoundException(
                        "Perfil não encontrado. Crie seu perfil com PUT /api/profile."));
    }

    @Transactional(readOnly = true)
    public boolean existsForUser(Long userId) {
        return profileRepository.findByUserId(userId).isPresent();
    }

    @Transactional
    public Profile upsert(Long userId, ProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.getReferenceById(userId);
            return new Profile(user);
        });

        profile.setWeightKg(request.weightKg());
        profile.setHeightCm(request.heightCm());
        profile.setAge(request.age());
        profile.setSex(request.sex());
        profile.setActivityLevel(request.activityLevel());
        profile.setGoal(request.goal());
        profile.setDietaryRestrictions(blankToNull(request.dietaryRestrictions()));
        profile.setMealsPerDay(request.mealsPerDay());
        profile.setBodyFatPercent(request.bodyFatPercent());
        profile.setFavoriteFoods(blankToNull(request.favoriteFoods()));
        profile.setDislikedFoods(blankToNull(request.dislikedFoods()));
        profile.setBudget(request.budget());
        profile.setRegion(request.region());
        profile.setMaxPrepMinutes(request.maxPrepMinutes());
        profile.setEatsOutAtLunch(request.eatsOutAtLunch());

        return profileRepository.save(profile);
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
