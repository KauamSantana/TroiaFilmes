package br.edu.unesc.troiafilms.entity;

/**
 * Papel de um usuário no sistema. Usado pelo Spring Security para
 * autorização baseada em papéis (prefixados automaticamente com "ROLE_").
 */
public enum Role {

    /** Gerencia o catálogo (filmes, categorias, planos) e consulta usuários. */
    ADMIN,

    /** Assinante comum: gerencia apenas os próprios perfis e dados de consumo. */
    USER
}
