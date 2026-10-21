import { useEffect, useState } from 'react'
import { categorias as apiCategorias, mensagemDeErro } from '../../services/api'
import Aviso, { Carregando } from '../../components/Aviso'
import Modal from '../../components/Modal'

const FORM_VAZIO = { nome: '', descricao: '' }

export default function PainelCategorias() {
  const [lista, setLista] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [emEdicao, setEmEdicao] = useState(null)
  const [form, setForm] = useState(FORM_VAZIO)

  const carregar = () => {
    setCarregando(true)
    apiCategorias
      .listar()
      .then(({ data }) => setLista(data))
      .catch((falha) => setErro(mensagemDeErro(falha)))
      .finally(() => setCarregando(false))
  }

  useEffect(carregar, [])

  const abrir = (categoria = null) => {
    setEmEdicao(categoria)
    setForm(categoria ? { nome: categoria.nome, descricao: categoria.descricao || '' } : FORM_VAZIO)
    setModalAberto(true)
  }

  const salvar = async (evento) => {
    evento.preventDefault()
    setErro('')
    setSucesso('')
    try {
      if (emEdicao) {
        await apiCategorias.atualizar(emEdicao.id, form)
        setSucesso('Categoria atualizada.')
      } else {
        await apiCategorias.criar(form)
        setSucesso('Categoria criada.')
      }
      setModalAberto(false)
      carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
      setModalAberto(false)
    }
  }

  const remover = async (categoria) => {
    if (!window.confirm(`Excluir a categoria "${categoria.nome}"?`)) return
    setErro('')
    setSucesso('')
    try {
      await apiCategorias.remover(categoria.id)
      setSucesso('Categoria removida.')
      carregar()
    } catch (falha) {
      // Regra 7: categoria com filmes vinculados devolve 409.
      setErro(mensagemDeErro(falha))
    }
  }

  if (carregando) return <Carregando />

  return (
    <>
      <Aviso tipo="erro">{erro}</Aviso>
      <Aviso tipo="sucesso">{sucesso}</Aviso>

      <div className="titulo-secao">
        <p className="subtitulo">{lista.length} categoria(s)</p>
        <button type="button" className="botao" onClick={() => abrir()}>
          Nova categoria
        </button>
      </div>

      <div className="tabela-container">
        <table>
          <thead>
            <tr>
              <th>Id</th>
              <th>Nome</th>
              <th style={{ whiteSpace: 'normal' }}>Descrição</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {lista.map((categoria) => (
              <tr key={categoria.id}>
                <td>{categoria.id}</td>
                <td>{categoria.nome}</td>
                <td style={{ whiteSpace: 'normal' }}>{categoria.descricao || '—'}</td>
                <td>
                  <div className="acoes-linha">
                    <button
                      type="button"
                      className="botao secundario pequeno"
                      onClick={() => abrir(categoria)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="botao perigo pequeno"
                      onClick={() => remover(categoria)}
                    >
                      Excluir
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {modalAberto && (
        <Modal
          titulo={emEdicao ? 'Editar categoria' : 'Nova categoria'}
          aoFechar={() => setModalAberto(false)}
        >
          <form onSubmit={salvar}>
            <div className="campo">
              <label htmlFor="cat-nome">Nome</label>
              <input
                id="cat-nome"
                value={form.nome}
                onChange={(ev) => setForm({ ...form, nome: ev.target.value })}
                minLength={2}
                maxLength={80}
                required
              />
            </div>
            <div className="campo">
              <label htmlFor="cat-desc">Descrição</label>
              <textarea
                id="cat-desc"
                value={form.descricao}
                onChange={(ev) => setForm({ ...form, descricao: ev.target.value })}
                maxLength={255}
              />
            </div>
            <div className="modal-acoes">
              <button type="button" className="botao secundario" onClick={() => setModalAberto(false)}>
                Cancelar
              </button>
              <button type="submit" className="botao">
                Salvar
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  )
}
