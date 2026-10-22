import { useCallback, useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import {
  avaliacoes as apiAvaliacoes,
  filmes as apiFilmes,
  historico as apiHistorico,
  mensagemDeErro,
  minhaLista as apiLista,
} from '../services/api'
import { useAuth } from '../context/AuthContext'
import Poster from '../components/Poster'
import Estrelas from '../components/Estrelas'
import Aviso, { Carregando } from '../components/Aviso'
import Player from '../components/Player'

/**
 * Tela de detalhe: concentra a maior parte das regras de negocio visiveis
 * ao usuario (avaliar uma unica vez, Minha Lista, registro de progresso).
 *
 * Aberta com ?assistir=1 (vindo do "Continuar assistindo"), ja abre o
 * player no ponto em que o perfil parou.
 */
export default function DetalheFilme() {
  const { id } = useParams()
  const { perfilAtivo } = useAuth()
  const [parametros] = useSearchParams()
  // Lido uma unica vez: abrir o player so na chegada, nao a cada recarga.
  const abrirAoCarregar = useRef(parametros.has('assistir'))

  const [filme, setFilme] = useState(null)
  const [listaAvaliacoes, setListaAvaliacoes] = useState([])
  const [naLista, setNaLista] = useState(false)
  const [minhaAvaliacao, setMinhaAvaliacao] = useState(null)
  const [progresso, setProgresso] = useState(null)
  const [player, setPlayer] = useState(null)
  const [carregando, setCarregando] = useState(true)

  const [nota, setNota] = useState(5)
  const [comentario, setComentario] = useState('')
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  const carregar = useCallback(async () => {
    setCarregando(true)
    try {
      const [detalhe, avaliacoesDoFilme, estaNaLista, minhas, historico] = await Promise.all([
        apiFilmes.porId(id),
        apiFilmes.avaliacoes(id),
        apiLista.contem(perfilAtivo.id, id),
        apiAvaliacoes.doPerfil(perfilAtivo.id),
        apiHistorico.listar(perfilAtivo.id),
      ])

      setFilme(detalhe.data)
      const registro = historico.data.find((h) => String(h.filme.id) === String(id)) ?? null
      setProgresso(registro)
      if (abrirAoCarregar.current) {
        abrirAoCarregar.current = false
        setPlayer({ inicio: pontoDePartida(registro) })
      }
      setListaAvaliacoes(avaliacoesDoFilme.data.conteudo)
      setNaLista(estaNaLista.data.naLista)

      const propria = minhas.data.find((a) => String(a.filmeId) === String(id))
      setMinhaAvaliacao(propria ?? null)
      if (propria) {
        setNota(propria.nota)
        setComentario(propria.comentario || '')
      }
      setErro('')
    } catch (falha) {
      setErro(mensagemDeErro(falha, 'Não foi possível carregar o filme.'))
    } finally {
      setCarregando(false)
    }
  }, [id, perfilAtivo.id])

  useEffect(() => {
    carregar()
  }, [carregar])

  const limparMensagens = () => {
    setErro('')
    setSucesso('')
  }

  const alternarLista = async () => {
    limparMensagens()
    try {
      if (naLista) {
        await apiLista.remover(perfilAtivo.id, id)
        setNaLista(false)
        setSucesso('Removido da sua lista.')
      } else {
        await apiLista.adicionar(perfilAtivo.id, id)
        setNaLista(true)
        setSucesso('Adicionado à sua lista.')
      }
    } catch (falha) {
      setErro(mensagemDeErro(falha))
    }
  }

  const assistir = () => {
    limparMensagens()
    setPlayer({ inicio: pontoDePartida(progresso) })
  }

  /** Ao sair do player, grava ate onde o perfil assistiu. */
  const sairDoPlayer = async (minutos) => {
    setPlayer(null)
    if (minutos <= 0) return
    try {
      const { data } = await apiHistorico.registrarProgresso(perfilAtivo.id, {
        filmeId: Number(id),
        minutosAssistidos: minutos,
      })
      setProgresso(data)
      setSucesso(
        data.concluido
          ? 'Filme concluído. Ele aparece em "Já assistidos".'
          : `Você parou em ${minutos} min. O filme aparece em "Continuar assistindo".`,
      )
    } catch (falha) {
      setErro(mensagemDeErro(falha))
    }
  }

  const enviarAvaliacao = async (evento) => {
    evento.preventDefault()
    limparMensagens()
    try {
      const dados = { filmeId: Number(id), nota, comentario: comentario || null }

      if (minhaAvaliacao) {
        await apiAvaliacoes.atualizar(minhaAvaliacao.id, dados)
        setSucesso('Avaliação atualizada.')
      } else {
        await apiAvaliacoes.criar(perfilAtivo.id, dados)
        setSucesso('Avaliação registrada.')
      }
      await carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
    }
  }

  const removerAvaliacao = async () => {
    limparMensagens()
    try {
      await apiAvaliacoes.remover(minhaAvaliacao.id)
      setMinhaAvaliacao(null)
      setNota(5)
      setComentario('')
      setSucesso('Avaliação removida.')
      await carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
    }
  }

  if (carregando) return <Carregando texto="Carregando filme..." />
  if (!filme) return <Aviso tipo="erro">{erro || 'Filme não encontrado.'}</Aviso>

  return (
    <>
      <Aviso tipo="erro">{erro}</Aviso>
      <Aviso tipo="sucesso">{sucesso}</Aviso>

      <div className="detalhe">
        <Poster titulo={filme.titulo} url={filme.posterUrl} />

        <div>
          <h1>{filme.titulo}</h1>
          <p className="subtitulo">
            {filme.anoLancamento} &middot; {filme.duracaoMinutos} min
            {filme.notaMedia
              ? ` · ★ ${filme.notaMedia.toFixed(1)} (${filme.totalAvaliacoes} avaliações)`
              : ' · ainda sem avaliações'}
          </p>

          <div className="etiquetas">
            {filme.categorias.map((categoria) => (
              <span key={categoria.id} className="etiqueta">
                {categoria.nome}
              </span>
            ))}
          </div>

          <p>{filme.sinopse}</p>

          <div className="acoes-detalhe">
            <button type="button" className="botao" onClick={assistir}>
              {rotuloAssistir(progresso)}
            </button>
            <button
              type="button"
              className={naLista ? 'botao perigo' : 'botao secundario'}
              onClick={alternarLista}
            >
              {naLista ? '− Remover da Minha Lista' : '+ Minha Lista'}
            </button>
          </div>
          {progresso && !progresso.concluido && progresso.minutosAssistidos > 0 && (
            <div className="barra-progresso" title={`${progresso.percentualAssistido}% assistido`}>
              <span style={{ width: `${progresso.percentualAssistido}%` }} />
            </div>
          )}
        </div>
      </div>

      {player && <Player filme={filme} inicio={player.inicio} aoSair={sairDoPlayer} />}

      <div className="bloco">
        <h2>{minhaAvaliacao ? 'Sua avaliação' : 'Avaliar este filme'}</h2>
        <p className="subtitulo">
          Cada perfil avalia um filme uma única vez. Avaliando como{' '}
          <strong>{perfilAtivo.nome}</strong>.
        </p>

        <form onSubmit={enviarAvaliacao}>
          <div className="campo">
            <label>Nota</label>
            <Estrelas nota={nota} aoSelecionar={setNota} />
          </div>

          <div className="campo">
            <label htmlFor="comentario">Comentário (opcional)</label>
            <textarea
              id="comentario"
              value={comentario}
              onChange={(ev) => setComentario(ev.target.value)}
              maxLength={2000}
              placeholder="O que você achou?"
            />
          </div>

          <div className="acoes-linha">
            <button type="submit" className="botao">
              {minhaAvaliacao ? 'Atualizar avaliação' : 'Enviar avaliação'}
            </button>
            {minhaAvaliacao && (
              <button type="button" className="botao perigo" onClick={removerAvaliacao}>
                Excluir avaliação
              </button>
            )}
          </div>
        </form>
      </div>

      <div className="bloco">
        <h2>Avaliações ({listaAvaliacoes.length})</h2>
        {listaAvaliacoes.length === 0 ? (
          <p className="subtitulo">Ainda ninguém avaliou este filme.</p>
        ) : (
          listaAvaliacoes.map((avaliacao) => (
            <div key={avaliacao.id} className="avaliacao-item">
              <div className="avaliacao-cabecalho">
                <strong>{avaliacao.perfilNome}</strong>
                <Estrelas nota={avaliacao.nota} />
              </div>
              <p className="subtitulo" style={{ margin: 0 }}>
                {avaliacao.comentario || 'Sem comentário.'}
              </p>
            </div>
          ))
        )}
      </div>
    </>
  )
}

/** Filme em andamento continua de onde parou; concluido ou nunca visto comeca do zero. */
function pontoDePartida(registro) {
  return registro && !registro.concluido ? registro.minutosAssistidos : 0
}

function rotuloAssistir(registro) {
  if (!registro || registro.minutosAssistidos === 0) return '▶ Assistir'
  if (registro.concluido) return '▶ Assistir de novo'
  return `▶ Continuar de ${registro.minutosAssistidos} min`
}
