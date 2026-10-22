package br.edu.unesc.troiafilmes.entity;

/** Situação de uma assinatura ao longo do seu ciclo de vida. */
public enum StatusAssinatura {

    /** Vigente. Cada usuário pode ter no máximo uma assinatura neste estado. */
    ATIVA,

    /** Encerrada porque o assinante contratou outro plano. */
    CANCELADA
}
