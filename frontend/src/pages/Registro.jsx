import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { mensagemDeErro } from '../services/api'
import Aviso from '../components/Aviso'

export default function Registro() {
  const { cadastrar } = useAuth()
  const navegar = useNavigate()

  const [form, setForm] = useState({ nome: '', email: '', senha: '' })
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  const alterar = (campo) => (ev) => setForm({ ...form, [campo]: ev.target.value })

  const aoEnviar = async (evento) => {
    evento.preventDefault()
    setErro('')
    setEnviando(true)
    try {
      await cadastrar(form.nome, form.email, form.senha)
      // O backend ja cria a assinatura de entrada e o perfil Principal.
      navegar('/perfis', { replace: true })
    } catch (falha) {
      setErro(mensagemDeErro(falha, 'Não foi possível criar a conta.'))
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="tela-auth">
      <form className="cartao-auth" onSubmit={aoEnviar}>
        <div className="marca">
          Troia<span>Filmes</span>
        </div>
        <p className="subtitulo">Crie a sua conta e comece a assistir</p>

        <Aviso tipo="erro">{erro}</Aviso>

        <div className="campo">
          <label htmlFor="nome">Nome</label>
          <input id="nome" value={form.nome} onChange={alterar('nome')} minLength={3} required />
        </div>

        <div className="campo">
          <label htmlFor="email">E-mail</label>
          <input id="email" type="email" value={form.email} onChange={alterar('email')} required />
        </div>

        <div className="campo">
          <label htmlFor="senha">Senha</label>
          <input
            id="senha"
            type="password"
            value={form.senha}
            onChange={alterar('senha')}
            minLength={8}
            autoComplete="new-password"
            required
          />
        </div>

        <button type="submit" className="botao largo" disabled={enviando}>
          {enviando ? 'Criando conta...' : 'Criar conta'}
        </button>

        <p className="rodape-auth">
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </form>
    </div>
  )
}
