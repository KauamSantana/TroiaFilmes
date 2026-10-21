import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { armazenamento, autenticacao, perfis as apiPerfis } from '../services/api'

const AuthContext = createContext(null)

/**
 * Guarda a sessao (usuario autenticado + perfil selecionado) e mantem tudo
 * espelhado no localStorage, para que um F5 nao derrube o usuario.
 */
export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(() => armazenamento.getUsuario())
  const [perfilAtivo, setPerfilAtivo] = useState(null)
  const [carregandoPerfil, setCarregandoPerfil] = useState(true)

  // Ao recarregar a pagina, recupera o perfil que estava selecionado.
  useEffect(() => {
    const perfilSalvo = armazenamento.getPerfilId()

    if (!usuario || !perfilSalvo) {
      setCarregandoPerfil(false)
      return
    }

    apiPerfis
      .listar()
      .then(({ data }) => {
        const encontrado = data.find((p) => String(p.id) === String(perfilSalvo))
        if (encontrado) setPerfilAtivo(encontrado)
      })
      .catch(() => {
        // Perfil apagado em outra sessao: o usuario escolhe outro.
      })
      .finally(() => setCarregandoPerfil(false))
  }, [usuario])

  const entrar = useCallback(async (email, senha) => {
    const { data } = await autenticacao.login(email, senha)
    armazenamento.salvarSessao(data)
    setUsuario(data.usuario)
    return data.usuario
  }, [])

  // O cadastro so cria a conta (201 com nome e e-mail); quem entrega os
  // tokens e o login. Por isso, logo depois de cadastrar, ja entramos.
  const cadastrar = useCallback(
    async (nome, email, senha) => {
      await autenticacao.registrar(nome, email, senha)
      return entrar(email, senha)
    },
    [entrar],
  )

  const sair = useCallback(async () => {
    try {
      await autenticacao.logout()
    } catch {
      // Mesmo que o logout falhe no servidor, a sessao local termina aqui.
    }
    armazenamento.limpar()
    setUsuario(null)
    setPerfilAtivo(null)
  }, [])

  const selecionarPerfil = useCallback((perfil) => {
    armazenamento.setPerfilId(perfil.id)
    setPerfilAtivo(perfil)
  }, [])

  const trocarDePerfil = useCallback(() => setPerfilAtivo(null), [])

  const valor = useMemo(
    () => ({
      usuario,
      perfilAtivo,
      carregandoPerfil,
      autenticado: Boolean(usuario),
      ehAdmin: usuario?.role === 'ADMIN',
      entrar,
      cadastrar,
      sair,
      selecionarPerfil,
      trocarDePerfil,
    }),
    [usuario, perfilAtivo, carregandoPerfil, entrar, cadastrar, sair, selecionarPerfil, trocarDePerfil],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const contexto = useContext(AuthContext)
  if (!contexto) {
    throw new Error('useAuth precisa estar dentro de <AuthProvider>')
  }
  return contexto
}
