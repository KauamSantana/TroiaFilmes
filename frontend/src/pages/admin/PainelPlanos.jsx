import { useEffect, useState } from 'react'
import { mensagemDeErro, planos as apiPlanos } from '../../services/api'
import Aviso, { Carregando } from '../../components/Aviso'
import Modal from '../../components/Modal'

const FORM_VAZIO = {
  nome: '',
  descricao: '',
  preco: '0.00',
  maxPerfis: 1,
  qualidadeMaxima: 'HD',
  ativo: true,
}

export default function PainelPlanos() {
  const [lista, setLista] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [emEdicao, setEmEdicao] = useState(null)
  const [form, setForm] = useState(FORM_VAZIO)

  const carregar = () => {
    setCarregando(true)
    apiPlanos
      .listar(false)
      .then(({ data }) => setLista(data))
      .catch((falha) => setErro(mensagemDeErro(falha)))
      .finally(() => setCarregando(false))
  }

  useEffect(carregar, [])

  const abrir = (plano = null) => {
    setEmEdicao(plano)
    setForm(
      plano
        ? {
            nome: plano.nome,
            descricao: plano.descricao || '',
            preco: String(plano.preco),
            maxPerfis: plano.maxPerfis,
            qualidadeMaxima: plano.qualidadeMaxima,
            ativo: plano.ativo,
          }
        : FORM_VAZIO,
    )
    setModalAberto(true)
  }

  const salvar = async (evento) => {
    evento.preventDefault()
    setErro('')
    setSucesso('')
    try {
      const dados = {
        ...form,
        preco: Number(form.preco),
        maxPerfis: Number(form.maxPerfis),
        descricao: form.descricao || null,
      }
      if (emEdicao) {
        await apiPlanos.atualizar(emEdicao.id, dados)
        setSucesso('Plano atualizado.')
      } else {
        await apiPlanos.criar(dados)
        setSucesso('Plano criado.')
      }
      setModalAberto(false)
      carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
      setModalAberto(false)
    }
  }

  const remover = async (plano) => {
    if (!window.confirm(`Excluir o plano "${plano.nome}"?`)) return
    setErro('')
    setSucesso('')
    try {
      await apiPlanos.remover(plano.id)
      setSucesso('Plano removido.')
      carregar()
    } catch (falha) {
      // Plano com assinantes ativos devolve 409: a saida e desativar.
      setErro(mensagemDeErro(falha))
    }
  }

  if (carregando) return <Carregando />

  return (
    <>
      <Aviso tipo="erro">{erro}</Aviso>
      <Aviso tipo="sucesso">{sucesso}</Aviso>

      <div className="titulo-secao">
        <p className="subtitulo">{lista.length} plano(s), incluindo os desativados</p>
        <button type="button" className="botao" onClick={() => abrir()}>
          Novo plano
        </button>
      </div>

      <div className="tabela-container">
        <table>
          <thead>
            <tr>
              <th>Id</th>
              <th>Nome</th>
              <th>Preço</th>
              <th>Perfis</th>
              <th>Qualidade</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {lista.map((plano) => (
              <tr key={plano.id}>
                <td>{plano.id}</td>
                <td>{plano.nome}</td>
                <td>R$ {Number(plano.preco).toFixed(2).replace('.', ',')}</td>
                <td>{plano.maxPerfis}</td>
                <td>{plano.qualidadeMaxima}</td>
                <td>{plano.ativo ? 'Ativo' : 'Desativado'}</td>
                <td>
                  <div className="acoes-linha">
                    <button
                      type="button"
                      className="botao secundario pequeno"
                      onClick={() => abrir(plano)}
                    >
                      Editar
                    </button>
                    <button
                      type="button"
                      className="botao perigo pequeno"
                      onClick={() => remover(plano)}
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
        <Modal titulo={emEdicao ? 'Editar plano' : 'Novo plano'} aoFechar={() => setModalAberto(false)}>
          <form onSubmit={salvar}>
            <div className="campo">
              <label htmlFor="pl-nome">Nome</label>
              <input
                id="pl-nome"
                value={form.nome}
                onChange={(ev) => setForm({ ...form, nome: ev.target.value })}
                minLength={2}
                maxLength={60}
                required
              />
            </div>

            <div className="campo">
              <label htmlFor="pl-desc">Descrição</label>
              <input
                id="pl-desc"
                value={form.descricao}
                onChange={(ev) => setForm({ ...form, descricao: ev.target.value })}
                maxLength={255}
              />
            </div>

            <div className="linha-campos">
              <div className="campo">
                <label htmlFor="pl-preco">Preço mensal</label>
                <input
                  id="pl-preco"
                  type="number"
                  step="0.01"
                  min="0"
                  value={form.preco}
                  onChange={(ev) => setForm({ ...form, preco: ev.target.value })}
                  required
                />
              </div>

              <div className="campo">
                <label htmlFor="pl-perfis">Máximo de perfis</label>
                <input
                  id="pl-perfis"
                  type="number"
                  min={1}
                  max={10}
                  value={form.maxPerfis}
                  onChange={(ev) => setForm({ ...form, maxPerfis: ev.target.value })}
                  required
                />
              </div>

              <div className="campo">
                <label htmlFor="pl-qualidade">Qualidade</label>
                <select
                  id="pl-qualidade"
                  value={form.qualidadeMaxima}
                  onChange={(ev) => setForm({ ...form, qualidadeMaxima: ev.target.value })}
                >
                  <option value="SD">SD</option>
                  <option value="HD">HD</option>
                  <option value="UHD">UHD</option>
                </select>
              </div>
            </div>

            <div className="caixa-checkbox">
              <input
                id="pl-ativo"
                type="checkbox"
                checked={form.ativo}
                onChange={(ev) => setForm({ ...form, ativo: ev.target.checked })}
              />
              <label htmlFor="pl-ativo">Disponível para contratação</label>
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
