package com.gerador.dietas.service;

import com.gerador.dietas.domain.User;
import com.gerador.dietas.dto.AuthResponse;
import com.gerador.dietas.dto.LoginRequest;
import com.gerador.dietas.dto.MeResponse;
import com.gerador.dietas.dto.RegisterRequest;
import com.gerador.dietas.exception.EmailAlreadyExistsException;
import com.gerador.dietas.repository.UserRepository;
import com.gerador.dietas.security.AppUserPrincipal;
import com.gerador.dietas.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("E-mail já cadastrado");
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.name().trim());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        // O JWT já foi validado pelo filtro; se o id não existe mais, a conta foi
        // removida e o token não vale nada.
        return userRepository.findById(userId)
                .map(MeResponse::from)
                .orElseThrow(() -> new IllegalStateException("Usuário do token não existe mais"));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        String token = jwtService.generate(principal.getId(), principal.getUsername());
        return new AuthResponse(token, jwtService.getExpirationSeconds());
    }
}
