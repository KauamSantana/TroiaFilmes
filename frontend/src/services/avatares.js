/**
 * Avatares de perfil no estilo das plataformas de streaming: quadrado colorido
 * com uma carinha. Os arquivos ficam em public/avatars, dentro do proprio
 * frontend, entao aparecem mesmo sem internet.
 */
export const AVATARES = ['azul', 'amarelo', 'vermelho', 'verde', 'roxo'].map(
  (cor) => `/avatars/${cor}.svg`,
)

/**
 * Avatar a exibir para um perfil: o escolhido, se for um dos nossos; senao,
 * uma cor fixa derivada do id, para o mesmo perfil sempre ter o mesmo rosto.
 */
export function avatarDoPerfil(perfil) {
  if (perfil?.avatarUrl && AVATARES.includes(perfil.avatarUrl)) {
    return perfil.avatarUrl
  }
  return AVATARES[Math.abs(Number(perfil?.id) || 0) % AVATARES.length]
}
