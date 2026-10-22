import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { mensagemDeErro, perfis as apiPerfis, assinaturas } from '../services/api'
import Aviso, { Carregando } from '../components/Aviso'
import Modal from '../components/Modal'
import { AVATARES, avatarDoPerfil } from '../services/avatares'

/**
 * Tela de escolha de perfil, no modelo dos servicos de streaming.
 *
 * Tambem e onde a regra de limite de perfis por plano aparece para o
 * usuario: o botao de criar some quando o limite e atingido, e a tentativa
 * pela API devolve 409 com a mensagem explicando o plano atual.
 */
export default function SelecaoPerfil() {
  const { selecionarPerfil, sair } = useAuth()
  const navegar = useNavigate()

  const [lista, setLista] = useState([])
  const [plano, setPlano] = useState(null)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [emEdicao, setEmEdicao] = useState(null)
  const [form, setForm] = useState({ nome: '', avatarUrl: '', infantil: false })
  const [salvando, setSalvando] = useState(false)

  const carregar = async () => {
    setCarregando(true)
    try {
      const [{ data: perfisData }, resultadoPlano] = await Promise.all([
        apiPerfis.listar(),
        assinaturas.ativa().catch(() => null),
      ])
      setLista(perfisData)
      setPlano(resultadoPlano?.data?.plano ?? null)
      setErro('')
    } catch (falha) {
      setErro(mensagemDeErro(falha, 'Não foi possível carregar os perfis.'))
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    carregar()
  }, [])

  const escolher = (perfil) => {
    selecionarPerfil(perfil)
    navegar('/catalogo')
  }

  const abrirCriacao = () => {
    setEmEdicao(null)
    // Sugere a primeira cor que ainda nao esta em uso na conta.
    const emUso = lista.map(avatarDoPerfil)
    const livre = AVATARES.find((a) => !emUso.includes(a)) ?? AVATARES[0]
    setForm({ nome: '', avatarUrl: livre, infantil: false })
    setModalAberto(true)
  }

  const abrirEdicao = (perfil, evento) => {
    evento.stopPropagation()
    setEmEdicao(perfil)
    setForm({
      nome: perfil.nome,
      avatarUrl: avatarDoPerfil(perfil),
      infantil: perfil.infantil,
    })
    setModalAberto(true)
  }

  const salvar = async (evento) => {
    evento.preventDefault()
    setSalvando(true)
    setErro('')
    try {
      const dados = { ...form, avatarUrl: form.avatarUrl || null }
      if (emEdicao) {
        await apiPerfis.atualizar(emEdicao.id, dados)
      } else {
        await apiPerfis.criar(dados)
      }
      setModalAberto(false)
      await carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha, 'Não foi possível salvar o perfil.'))
      setModalAberto(false)
    } finally {
      setSalvando(false)
    }
  }

  const excluir = async (perfil, evento) => {
    evento.stopPropagation()
    if (!window.confirm(`Excluir o perfil "${perfil.nome}"? As avaliações e o histórico dele serão perdidos.`)) {
      return
    }
    try {
      await apiPerfis.remover(perfil.id)
      await carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha, 'Não foi possível excluir o perfil.'))
    }
  }

  const limite = plano?.maxPerfis ?? 1
  const podeCriar = lista.length < limite

  if (carregando) return <Carregando texto="Carregando perfis..." />

  return (
    <div className="tela-auth">
      <div style={{ width: 'min(760px, 100%)' }}>
        <h1 className="centralizado">Quem está assistindo?</h1>
        <p className="subtitulo centralizado">
          {plano
            ? `Plano ${plano.nome} · ${lista.length} de ${limite} perfil(is) em uso`
            : 'Sem assinatura ativa · limitado a 1 perfil'}
        </p>

        <Aviso tipo="erro">{erro}</Aviso>

        <div className="grade-perfis">
          {lista.map((perfil) => (
            <div key={perfil.id} style={{ textAlign: 'center' }}>
              <button type="button" className="cartao-perfil" onClick={() => escolher(perfil)}>
                <img className="avatar-perfil" src={avatarDoPerfil(perfil)} alt="" />
                <span>
                  {perfil.nome}{' '}
                  {perfil.infantil && <span className="etiqueta-infantil">infantil</span>}
                </span>
              </button>
              <div className="acoes-linha" style={{ justifyContent: 'center', marginTop: '0.4rem' }}>
                <button
                  type="button"
                  className="botao secundario pequeno"
                  onClick={(ev) => abrirEdicao(perfil, ev)}
                >
                  Editar
                </button>
                <button
                  type="button"
                  className="botao perigo pequeno"
                  onClick={(ev) => excluir(perfil, ev)}
                >
                  Excluir
                </button>
              </div>
            </div>
          ))}

          {podeCriar && (
            <button type="button" className="cartao-perfil" onClick={abrirCriacao}>
              <span className="avatar-perfil">+</span>
              <span>Adicionar perfil</span>
            </button>
          )}
        </div>

        {!podeCriar && (
          <Aviso tipo="neutro">
            Você atingiu o limite de {limite} perfil(is) do plano {plano?.nome}. Faça upgrade em
            Planos para criar mais.
          </Aviso>
        )}

        <div className="centralizado">
          <button type="button" className="botao secundario" onClick={sair}>
            Sair da conta
          </button>
        </div>
      </div>

      {modalAberto && (
        <Modal
          titulo={emEdicao ? 'Editar perfil' : 'Novo perfil'}
          aoFechar={() => setModalAberto(false)}
        >
          <form onSubmit={salvar}>
            <div className="campo">
              <label htmlFor="perfil-nome">Nome do perfil</label>
              <input
                id="perfil-nome"
                value={form.nome}
                onChange={(ev) => setForm({ ...form, nome: ev.target.value })}
                minLength={2}
                maxLength={60}
                required
              />
            </div>

            <div className="campo">
              <label>Avatar</label>
              <div className="escolha-avatar">
                {AVATARES.map((avatar) => (
                  <button
                    key={avatar}
                    type="button"
                    className={form.avatarUrl === avatar ? 'selecionado' : ''}
                    onClick={() => setForm({ ...form, avatarUrl: avatar })}
                    aria-label={`Escolher o avatar ${avatar.split('/').pop().replace('.svg', '')}`}
                  >
                    <img src={avatar} alt="" />
                  </button>
                ))}
              </div>
            </div>

            <div className="caixa-checkbox">
              <input
                id="perfil-infantil"
                type="checkbox"
                checked={form.infantil}
                onChange={(ev) => setForm({ ...form, infantil: ev.target.checked })}
              />
              <label htmlFor="perfil-infantil">Perfil infantil</label>
            </div>

            <div className="modal-acoes">
              <button
                type="button"
                className="botao secundario"
                onClick={() => setModalAberto(false)}
              >
                Cancelar
              </button>
              <button type="submit" className="botao" disabled={salvando}>
                {salvando ? 'Salvando...' : 'Salvar'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  )
}
