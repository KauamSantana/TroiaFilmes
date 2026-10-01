package br.edu.unesc.troiafilmes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Conta de acesso ao TroiaFilmes.
 *
 * <p>Relacionamentos: um usuário possui vários perfis (1:N) e várias
 * assinaturas ao longo do tempo (1:N), das quais no máximo uma fica ATIVA.</p>
 *
 * <p>Implementa {@link UserDetails} para que o Spring Security autentique
 * direto pela entidade: o login é o e-mail, a senha é o hash BCrypt e o
 * papel vira a autoridade {@code ROLE_ADMIN} ou {@code ROLE_USER}.</p>
 */
@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "nome", "email", "role"})
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    /** Hash BCrypt. A senha em texto puro nunca é persistida nem retornada. */
    @Column(nullable = false, length = 100)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    /** Soft delete: um usuário inativo não consegue autenticar. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Perfil> perfis = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Assinatura> assinaturas = new ArrayList<>();

    /** Conveniência para as regras de autorização. */
    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    // ------------------------------------------------------ UserDetails

    /** O prefixo ROLE_ é a convenção que {@code hasRole("ADMIN")} espera encontrar. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    /** O e-mail é o identificador de login do TroiaFilmes. */
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Conta desativada (soft delete) não autentica, mesmo com a senha certa. */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(ativo);
    }
}
