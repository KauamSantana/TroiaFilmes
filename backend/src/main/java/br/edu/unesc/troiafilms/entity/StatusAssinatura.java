package br.edu.unesc.troiafilms.entity;

/** Situação de uma assinatura ao longo do seu ciclo de vida. */
public enum StatusAssinatura {

    /** Vigente. Cada usuário pode ter no máximo uma assinatura neste estado. */
    ATIVA,

    /** Encerrada pelo próprio assinante ou substituída por outro plano. */
    CANCELADA,

    /** Encerrada porque a data de fim foi atingida. */
    EXPIRADA
}
