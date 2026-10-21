import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { Carregando } from './Aviso'

/** Exige sessao ativa; sem ela, manda para o login guardando o destino. */
export function RotaAutenticada() {
  const { autenticado } = useAuth()
  const local = useLocation()

  if (!autenticado) {
    return <Navigate to="/login" state={{ de: local.pathname }} replace />
  }
  return <Outlet />
}

/**
 * Exige, alem da sessao, um perfil selecionado. As telas de consumo
 * (catalogo, lista, historico) sao sempre no contexto de um perfil.
 */
export function RotaComPerfil() {
  const { perfilAtivo, carregandoPerfil } = useAuth()

  if (carregandoPerfil) {
    return <Carregando texto="Recuperando o seu perfil..." />
  }
  if (!perfilAtivo) {
    return <Navigate to="/perfis" replace />
  }
  return <Outlet />
}

/** Exige papel ADMIN. */
export function RotaAdmin() {
  const { ehAdmin } = useAuth()

  if (!ehAdmin) {
    return <Navigate to="/catalogo" replace />
  }
  return <Outlet />
}
