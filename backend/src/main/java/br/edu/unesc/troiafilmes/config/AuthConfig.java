package br.edu.unesc.troiafilmes.config;

import br.edu.unesc.troiafilmes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ensina o Spring Security a encontrar um usuário: pelo e-mail, na tabela
 * {@code usuario}. É o que o {@code AuthenticationManager} usa no login para
 * buscar a conta e comparar a senha com o hash BCrypt.
 */
@Service
@RequiredArgsConstructor
public class AuthConfig implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
