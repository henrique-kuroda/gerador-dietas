package com.gerador.dietas.dto;

import com.gerador.dietas.domain.User;

public record MeResponse(Long id, String name, String email) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getName(), user.getEmail());
    }
}
