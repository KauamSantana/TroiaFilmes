import { useEffect, useState } from 'react'
import {
  categorias as apiCategorias,
  filmes as apiFilmes,
  mensagemDeErro,
} from '../../services/api'
import Aviso, { Carregando } from '../../components/Aviso'
import Modal from '../../components/Modal'

const FORM_VAZIO = {
  titulo: '',
  sinopse: '',
  anoLancamento: new Date().getFullYear(),
  duracaoMinutos: 100,
  posterUrl: '',
  categoriaIds: [],
}

/** CRUD completo do catálogo, incluindo o vínculo N:N com categorias. */
export default function PainelFilmes() {
  const [pagina, setPagina] = useState(null)
  const [listaCategorias, setListaCategorias] = useState([])
  const [numeroPagina, setNumeroPagina] = useState(0)
  const [busca, setBusca] = useState('')
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')
  const [modalAberto, setModalAberto] = useState(false)
  const [emEdicao, setEmEdicao] = useState(null)
  const [form, setForm] = useState(FORM_VAZIO)
  const [salvando, setSalvando] = useState(false)
  const [enviandoImagem, setEnviandoImagem] = useState(false)
  // Aviso proprio do bloco de poster: o modal cobre a tela, entao as faixas
  // de erro e sucesso da pagina ficariam escondidas atras dele.
  const [avisoImagem, setAvisoImagem] = useState(null)

  useEffect(() => {
    apiCategorias.listar().then(({ data }) => setListaCategorias(data)).catch(() => {})
  }, [])

  const carregar = () => {
    setCarregando(true)
    const params = { page: numeroPagina, size: 10 }
    if (busca) params.titulo = busca

    apiFilmes
      .buscar(params)
      .then(({ data }) => setPagina(data))
      .catch((falha) => setErro(mensagemDeErro(falha)))
      .finally(() => setCarregando(false))
  }

  // 'busca' fica de fora de proposito: a pesquisa dispara no submit do
  // formulario, nao a cada tecla digitada.
  // oxlint-disable-next-line react-hooks/exhaustive-deps
  useEffect(carregar, [numeroPagina])

  const pesquisar = (evento) => {
    evento.preventDefault()
    if (numeroPagina === 0) carregar()
    else setNumeroPagina(0)
  }

  const abrir = (filme = null) => {
    setAvisoImagem(null)
    setEmEdicao(filme)
    setForm(
      filme
        ? {
            titulo: filme.titulo,
            sinopse: filme.sinopse || '',
            anoLancamento: filme.anoLancamento,
            duracaoMinutos: filme.duracaoMinutos,
            posterUrl: filme.posterUrl || '',
            categoriaIds: filme.categorias.map((c) => c.id),
          }
        : FORM_VAZIO,
    )
    setModalAberto(true)
  }

  /**
   * Envia a imagem e ja reflete a URL devolvida pela API no formulario.
   *
   * Diferente do resto do formulario, isto grava na hora: o endpoint de
   * poster recebe o id do filme, entao so existe para filme ja salvo.
   */
  const enviarImagem = async (arquivo) => {
    if (!arquivo || !emEdicao) return

    setAvisoImagem(null)
    setEnviandoImagem(true)
    try {
      const { data } = await apiFilmes.enviarPoster(emEdicao.id, arquivo)
      setForm((atual) => ({ ...atual, posterUrl: data.posterUrl || '' }))
      setAvisoImagem({ tipo: 'sucesso', texto: 'Imagem enviada e aplicada ao filme.' })
      carregar()
    } catch (falha) {
      setAvisoImagem({ tipo: 'erro', texto: mensagemDeErro(falha, 'Falha ao enviar a imagem.') })
    } finally {
      setEnviandoImagem(false)
    }
  }

  const removerImagem = async () => {
    if (!emEdicao) return

    setAvisoImagem(null)
    setEnviandoImagem(true)
    try {
      await apiFilmes.removerPoster(emEdicao.id)
      setForm((atual) => ({ ...atual, posterUrl: '' }))
      setAvisoImagem({ tipo: 'sucesso', texto: 'Imagem removida.' })
      carregar()
    } catch (falha) {
      setAvisoImagem({ tipo: 'erro', texto: mensagemDeErro(falha, 'Falha ao remover a imagem.') })
    } finally {
      setEnviandoImagem(false)
    }
  }

  const alternarCategoria = (id) => {
    setForm((atual) => ({
      ...atual,
      categoriaIds: atual.categoriaIds.includes(id)
        ? atual.categoriaIds.filter((c) => c !== id)
        : [...atual.categoriaIds, id],
    }))
  }

  const salvar = async (evento) => {
    evento.preventDefault()
    setErro('')
    setSucesso('')
    setSalvando(true)
    try {
      const dados = {
        ...form,
        anoLancamento: Number(form.anoLancamento),
        duracaoMinutos: Number(form.duracaoMinutos),
        posterUrl: form.posterUrl || null,
        sinopse: form.sinopse || null,
      }

      if (emEdicao) {
        await apiFilmes.atualizar(emEdicao.id, dados)
        setSucesso('Filme atualizado.')
      } else {
        await apiFilmes.criar(dados)
        setSucesso('Filme cadastrado.')
      }
      setModalAberto(false)
      carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
      setModalAberto(false)
    } finally {
      setSalvando(false)
    }
  }

  const remover = async (filme) => {
    if (!window.confirm(`Excluir "${filme.titulo}"? As avaliações e históricos vão junto.`)) return
    setErro('')
    setSucesso('')
    try {
      await apiFilmes.remover(filme.id)
      setSucesso('Filme removido.')
      carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
    }
  }

  return (
    <>
      <Aviso tipo="erro">{erro}</Aviso>
      <Aviso tipo="sucesso">{sucesso}</Aviso>

      <div className="titulo-secao">
        <form onSubmit={pesquisar} className="acoes-linha">
          <input
            value={busca}
            onChange={(ev) => setBusca(ev.target.value)}
            placeholder="Buscar por título"
          />
          <button type="submit" className="botao secundario">
            Buscar
          </button>
        </form>
        <button type="button" className="botao" onClick={() => abrir()}>
          Novo filme
        </button>
      </div>

      {carregando ? (
        <Carregando />
      ) : (
        <>
          <div className="tabela-container">
            <table>
              <thead>
                <tr>
                  <th>Id</th>
                  <th>Título</th>
                  <th>Ano</th>
                  <th>Duração</th>
                  <th>Categorias</th>
                  <th>Nota</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {pagina?.conteudo?.map((filme) => (
                  <tr key={filme.id}>
                    <td>{filme.id}</td>
                    <td>{filme.titulo}</td>
                    <td>{filme.anoLancamento}</td>
                    <td>{filme.duracaoMinutos} min</td>
                    <td style={{ whiteSpace: 'normal' }}>
                      {filme.categorias.map((c) => c.nome).join(', ')}
                    </td>
                    <td>
                      {filme.notaMedia ? `★ ${filme.notaMedia.toFixed(1)} (${filme.totalAvaliacoes})` : '—'}
                    </td>
                    <td>
                      <div className="acoes-linha">
                        <button
                          type="button"
                          className="botao secundario pequeno"
                          onClick={() => abrir(filme)}
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          className="botao perigo pequeno"
                          onClick={() => remover(filme)}
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

          {pagina && pagina.totalPaginas > 1 && (
            <div className="paginacao">
              <button
                type="button"
                className="botao secundario pequeno"
                onClick={() => setNumeroPagina((n) => n - 1)}
                disabled={numeroPagina === 0}
              >
                Anterior
              </button>
              <span>
                Página {pagina.pagina + 1} de {pagina.totalPaginas} &middot; {pagina.totalElementos}{' '}
                filme(s)
              </span>
              <button
                type="button"
                className="botao secundario pequeno"
                onClick={() => setNumeroPagina((n) => n + 1)}
                disabled={pagina.ultima}
              >
                Próxima
              </button>
            </div>
          )}
        </>
      )}

      {modalAberto && (
        <Modal
          titulo={emEdicao ? `Editar "${emEdicao.titulo}"` : 'Novo filme'}
          aoFechar={() => setModalAberto(false)}
        >
          <form onSubmit={salvar}>
            <div className="campo">
              <label htmlFor="fl-titulo">Título</label>
              <input
                id="fl-titulo"
                value={form.titulo}
                onChange={(ev) => setForm({ ...form, titulo: ev.target.value })}
                maxLength={180}
                required
              />
            </div>

            <div className="campo">
              <label htmlFor="fl-sinopse">Sinopse</label>
              <textarea
                id="fl-sinopse"
                value={form.sinopse}
                onChange={(ev) => setForm({ ...form, sinopse: ev.target.value })}
                maxLength={4000}
              />
            </div>

            <div className="linha-campos">
              <div className="campo">
                <label htmlFor="fl-ano">Ano de lançamento</label>
                <input
                  id="fl-ano"
                  type="number"
                  value={form.anoLancamento}
                  onChange={(ev) => setForm({ ...form, anoLancamento: ev.target.value })}
                  min={1888}
                  max={2200}
                  required
                />
              </div>

              <div className="campo">
                <label htmlFor="fl-duracao">Duração (min)</label>
                <input
                  id="fl-duracao"
                  type="number"
                  value={form.duracaoMinutos}
                  onChange={(ev) => setForm({ ...form, duracaoMinutos: ev.target.value })}
                  min={1}
                  max={1000}
                  required
                />
              </div>
            </div>

            <div className="campo">
              <label htmlFor="fl-poster">Pôster</label>

              <div className="editor-poster">
                <div className="previa-poster">
                  {form.posterUrl ? (
                    <img src={form.posterUrl} alt="Prévia do pôster" />
                  ) : (
                    <span>sem imagem</span>
                  )}
                </div>

                <div className="editor-poster-controles">
                  <input
                    id="fl-poster"
                    value={form.posterUrl}
                    onChange={(ev) => setForm({ ...form, posterUrl: ev.target.value })}
                    placeholder="/posters/arquivo.jpg ou https://..."
                  />

                  {emEdicao ? (
                    <div className="acoes-linha">
                      <label className="botao secundario" style={{ cursor: 'pointer' }}>
                        {enviandoImagem ? 'Enviando…' : 'Enviar imagem'}
                        <input
                          type="file"
                          accept="image/jpeg,image/png,image/webp"
                          hidden
                          disabled={enviandoImagem}
                          onChange={(ev) => {
                            const arquivo = ev.target.files?.[0]
                            // Limpa o input para que escolher o MESMO arquivo
                            // de novo, apos um erro, dispare o onChange.
                            ev.target.value = ''
                            enviarImagem(arquivo)
                          }}
                        />
                      </label>

                      {form.posterUrl && (
                        <button
                          type="button"
                          className="botao secundario"
                          disabled={enviandoImagem}
                          onClick={removerImagem}
                        >
                          Remover imagem
                        </button>
                      )}
                    </div>
                  ) : (
                    <p className="dica">
                      Salve o filme primeiro para poder enviar uma imagem.
                    </p>
                  )}

                  {avisoImagem && (
                    <p className={`dica ${avisoImagem.tipo === 'erro' ? 'dica-erro' : ''}`}>
                      {avisoImagem.texto}
                    </p>
                  )}

                  <p className="dica">
                    JPEG, PNG ou WebP, até 2 MB. O envio grava na hora, mesmo que
                    você cancele o formulário depois.
                  </p>
                </div>
              </div>
            </div>

            <div className="campo">
              <label>Categorias (ao menos uma)</label>
              <div className="lista-selecao">
                {listaCategorias.map((categoria) => (
                  <label
                    key={categoria.id}
                    className={`opcao-selecao ${
                      form.categoriaIds.includes(categoria.id) ? 'marcada' : ''
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={form.categoriaIds.includes(categoria.id)}
                      onChange={() => alternarCategoria(categoria.id)}
                      style={{ width: 14, height: 14 }}
                    />
                    {categoria.nome}
                  </label>
                ))}
              </div>
            </div>

            <div className="modal-acoes">
              <button
                type="button"
                className="botao secundario"
                onClick={() => setModalAberto(false)}
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="botao"
                disabled={salvando || form.categoriaIds.length === 0}
              >
                {salvando ? 'Salvando...' : 'Salvar'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </>
  )
}
