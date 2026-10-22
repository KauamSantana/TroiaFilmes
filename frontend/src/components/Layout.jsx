import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { avatarDoPerfil } from '../services/avatares'
import { useAuth } from '../context/AuthContext'

/** Moldura das telas internas: cabecalho, navegacao e rodape. */
export default function Layout() {
  const { usuario, perfilAtivo, ehAdmin, sair, trocarDePerfil } = useAuth()
  const navegar = useNavigate()

  const aoSair = async () => {
    await sair()
    navegar('/login')
  }

  const aoTrocarPerfil = () => {
    trocarDePerfil()
    navegar('/perfis')
  }

  const classe = ({ isActive }) => (isActive ? 'ativo' : undefined)

  return (
    <div className="app">
      <header className="cabecalho">
        <NavLink to="/catalogo" className="marca">
          Troia<span>Filmes</span>
        </NavLink>

        <nav className="nav">
          <NavLink to="/catalogo" className={classe}>
            Catálogo
          </NavLink>
          <NavLink to="/minha-lista" className={classe}>
            Minha Lista
          </NavLink>
          <NavLink to="/continuar-assistindo" className={classe}>
            Continuar assistindo
          </NavLink>
          <NavLink to="/planos" className={classe}>
            Planos
          </NavLink>
          {ehAdmin && (
            <NavLink to="/admin" className={classe}>
              Administração
            </NavLink>
          )}
        </nav>

        <div className="cabecalho-direita">
          {perfilAtivo && (
            <button type="button" className="chip-perfil" onClick={aoTrocarPerfil}>
              <img src={avatarDoPerfil(perfilAtivo)} alt="" />
              {perfilAtivo.nome}
            </button>
          )}
          <button type="button" className="botao secundario pequeno" onClick={aoSair}>
            Sair
          </button>
        </div>
      </header>

      <main className="conteudo">
        <Outlet />
      </main>

      <footer className="rodape">
        TroiaFilmes &middot; Trabalho 1 de Desenvolvimento Backend com Spring &middot; UNESC
        {usuario && ` · autenticado como ${usuario.email} (${usuario.role})`}
      </footer>
    </div>
  )
}
