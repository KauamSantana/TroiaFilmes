import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { mensagemDeErro } from '../services/api'
import Aviso from '../components/Aviso'

export default function Login() {
  const { entrar } = useAuth()
  const navegar = useNavigate()
  const local = useLocation()

  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  const aoEnviar = async (evento) => {
    evento.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      await entrar(email, senha)
      navegar(local.state?.de || '/perfis', { replace: true })
    } catch (falha) {
      setErro(mensagemDeErro(falha, 'Não foi possível entrar.'))
    } finally {
      setEnviando(false)
    }
  }

  const preencher = (e, s) => {
    setEmail(e)
    setSenha(s)
  }

  return (
    <div className="tela-auth">
      <form className="cartao-auth" onSubmit={aoEnviar}>
        <div className="marca">
          Troia<span>Filmes</span>
        </div>
        <p className="subtitulo">Entre para continuar assistindo</p>

        <Aviso tipo="erro">{erro}</Aviso>

        <div className="campo">
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(ev) => setEmail(ev.target.value)}
            placeholder="voce@troiafilmes.com"
            autoComplete="username"
            required
          />
        </div>

        <div className="campo">
          <label htmlFor="senha">Senha</label>
          <input
            id="senha"
            type="password"
            value={senha}
            onChange={(ev) => setSenha(ev.target.value)}
            autoComplete="current-password"
            required
          />
        </div>

        <button type="submit" className="botao largo" disabled={enviando}>
          {enviando ? 'Entrando...' : 'Entrar'}
        </button>

        <p className="rodape-auth">
          Ainda não tem conta? <Link to="/registro">Cadastre-se</Link>
        </p>

        <div className="credenciais-demo">
          <strong>Credenciais de demonstração</strong>
          <br />
          ADMIN: admin@troiafilmes.com / Admin@123{' '}
          <button type="button" onClick={() => preencher('admin@troiafilmes.com', 'Admin@123')}>
            usar
          </button>
          <br />
          USER: usuario@troiafilmes.com / User@123{' '}
          <button type="button" onClick={() => preencher('usuario@troiafilmes.com', 'User@123')}>
            usar
          </button>
        </div>
      </form>
    </div>
  )
}
