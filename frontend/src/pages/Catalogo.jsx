import { useEffect, useState } from 'react'
import { categorias as apiCategorias, filmes as apiFilmes, mensagemDeErro } from '../services/api'
import CartaoFilme from '../components/CartaoFilme'
import Aviso, { Carregando, Vazio } from '../components/Aviso'

const TAMANHO_PAGINA = 12

/**
 * Catalogo com os tres filtros combinaveis expostos pela API
 * (titulo, categoria e ano) e paginacao no servidor.
 */
export default function Catalogo() {
  const [pagina, setPagina] = useState(null)
  const [listaCategorias, setListaCategorias] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const [filtros, setFiltros] = useState({ titulo: '', categoriaId: '', ano: '' })
  const [aplicados, setAplicados] = useState({ titulo: '', categoriaId: '', ano: '' })
  const [numeroPagina, setNumeroPagina] = useState(0)

  useEffect(() => {
    apiCategorias
      .listar()
      .then(({ data }) => setListaCategorias(data))
      .catch(() => setListaCategorias([]))
  }, [])

  useEffect(() => {
    let cancelado = false
    setCarregando(true)

    const params = { page: numeroPagina, size: TAMANHO_PAGINA }
    if (aplicados.titulo) params.titulo = aplicados.titulo
    if (aplicados.categoriaId) params.categoriaId = aplicados.categoriaId
    if (aplicados.ano) params.ano = aplicados.ano

    apiFilmes
      .buscar(params)
      .then(({ data }) => {
        if (!cancelado) {
          setPagina(data)
          setErro('')
        }
      })
      .catch((falha) => {
        if (!cancelado) setErro(mensagemDeErro(falha, 'Não foi possível carregar o catálogo.'))
      })
      .finally(() => {
        if (!cancelado) setCarregando(false)
      })

    return () => {
      cancelado = true
    }
  }, [aplicados, numeroPagina])

  const buscar = (evento) => {
    evento.preventDefault()
    setNumeroPagina(0)
    setAplicados(filtros)
  }

  const limpar = () => {
    const vazio = { titulo: '', categoriaId: '', ano: '' }
    setFiltros(vazio)
    setAplicados(vazio)
    setNumeroPagina(0)
  }

  const anos = gerarAnos()

  return (
    <>
      <div className="titulo-secao">
        <div>
          <h2>Catálogo</h2>
          <p className="subtitulo">
            {pagina ? `${pagina.totalElementos} título(s) disponível(is)` : 'Carregando...'}
          </p>
        </div>
      </div>

      <form className="filtros" onSubmit={buscar}>
        <div className="campo" style={{ marginBottom: 0 }}>
          <label htmlFor="f-titulo">Título</label>
          <input
            id="f-titulo"
            value={filtros.titulo}
            onChange={(ev) => setFiltros({ ...filtros, titulo: ev.target.value })}
            placeholder="Buscar por parte do título"
          />
        </div>

        <div className="campo" style={{ marginBottom: 0 }}>
          <label htmlFor="f-categoria">Categoria</label>
          <select
            id="f-categoria"
            value={filtros.categoriaId}
            onChange={(ev) => setFiltros({ ...filtros, categoriaId: ev.target.value })}
          >
            <option value="">Todas</option>
            {listaCategorias.map((categoria) => (
              <option key={categoria.id} value={categoria.id}>
                {categoria.nome}
              </option>
            ))}
          </select>
        </div>

        <div className="campo" style={{ marginBottom: 0 }}>
          <label htmlFor="f-ano">Ano</label>
          <select
            id="f-ano"
            value={filtros.ano}
            onChange={(ev) => setFiltros({ ...filtros, ano: ev.target.value })}
          >
            <option value="">Todos</option>
            {anos.map((ano) => (
              <option key={ano} value={ano}>
                {ano}
              </option>
            ))}
          </select>
        </div>

        <div className="acoes-linha">
          <button type="submit" className="botao">
            Filtrar
          </button>
          <button type="button" className="botao secundario" onClick={limpar}>
            Limpar
          </button>
        </div>
      </form>

      <Aviso tipo="erro">{erro}</Aviso>

      {carregando && <Carregando texto="Buscando filmes..." />}

      {!carregando && pagina?.conteudo?.length === 0 && (
        <Vazio>Nenhum filme encontrado com esses filtros.</Vazio>
      )}

      {!carregando && pagina?.conteudo?.length > 0 && (
        <>
          <div className="grade-filmes">
            {pagina.conteudo.map((filme) => (
              <CartaoFilme key={filme.id} filme={filme} />
            ))}
          </div>

          {pagina.totalPaginas > 1 && (
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
                Página {pagina.pagina + 1} de {pagina.totalPaginas}
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
    </>
  )
}

/** Lista de anos para o filtro, do ano corrente para tras. */
function gerarAnos() {
  const atual = new Date().getFullYear()
  return Array.from({ length: 30 }, (_, i) => atual - i)
}
