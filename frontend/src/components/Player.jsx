import { useEffect, useState } from 'react'

/** Quantos milissegundos de tela equivalem a 1 minuto de filme na simulacao. */
const MS_POR_MINUTO = 250

/**
 * Reproducao simulada de um filme.
 *
 * O TroiaFilmes nao transmite video: o que ele guarda e ate onde cada perfil
 * assistiu. Este player faz o tempo andar (1 minuto de filme a cada 0,25 s),
 * permite pausar, voltar, avancar e clicar na barra, e ao sair devolve o
 * minuto em que parou, para a tela salvar o progresso pela API.
 */
export default function Player({ filme, inicio = 0, aoSair }) {
  const duracao = filme.duracaoMinutos
  const [minuto, setMinuto] = useState(Math.min(inicio, duracao))
  const [tocando, setTocando] = useState(true)

  // O tempo anda enquanto estiver tocando; no fim do filme, para sozinho.
  useEffect(() => {
    if (!tocando || minuto >= duracao) return undefined
    const passo = setTimeout(() => setMinuto((m) => Math.min(duracao, m + 1)), MS_POR_MINUTO)
    return () => clearTimeout(passo)
  }, [tocando, minuto, duracao])

  // Esc sai do player, como nos servicos de streaming.
  useEffect(() => {
    const tecla = (ev) => {
      if (ev.key === 'Escape') aoSair(minuto)
    }
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [aoSair, minuto])

  const mover = (delta) => setMinuto((m) => Math.max(0, Math.min(duracao, m + delta)))

  const clicarNaBarra = (ev) => {
    const caixa = ev.currentTarget.getBoundingClientRect()
    const fracao = (ev.clientX - caixa.left) / caixa.width
    setMinuto(Math.round(Math.max(0, Math.min(1, fracao)) * duracao))
  }

  const percentual = duracao > 0 ? (minuto / duracao) * 100 : 0
  const terminou = minuto >= duracao

  return (
    <div className="player" role="dialog" aria-label={`Assistindo ${filme.titulo}`}>
      {filme.posterUrl && (
        <div className="player-fundo" style={{ backgroundImage: `url(${filme.posterUrl})` }} />
      )}

      <div className="player-topo">
        <button type="button" className="botao secundario" onClick={() => aoSair(minuto)}>
          ← Sair do filme
        </button>
        <span className="player-titulo">{filme.titulo}</span>
      </div>

      <div className="player-centro">
        {terminou ? (
          <p className="player-aviso">Fim do filme</p>
        ) : (
          <button
            type="button"
            className="player-play"
            onClick={() => setTocando((t) => !t)}
            aria-label={tocando ? 'Pausar' : 'Continuar'}
            title={tocando ? 'Pausar' : 'Continuar'}
          >
            {tocando ? '❚❚' : '▶'}
          </button>
        )}
      </div>

      <div className="player-rodape">
        <div className="player-barra" onClick={clicarNaBarra} title="Clique para pular para este ponto">
          <span style={{ width: `${percentual}%` }} />
        </div>
        <div className="player-controles">
          <div className="acoes-linha">
            <button type="button" className="botao secundario pequeno" onClick={() => mover(-10)}>
              « 10 min
            </button>
            <button
              type="button"
              className="botao secundario pequeno"
              onClick={() => setTocando((t) => !t)}
              disabled={terminou}
            >
              {tocando ? 'Pausar' : 'Continuar'}
            </button>
            <button type="button" className="botao secundario pequeno" onClick={() => mover(10)}>
              10 min »
            </button>
          </div>
          <span className="player-tempo">
            {minuto} min / {duracao} min
          </span>
        </div>
      </div>
    </div>
  )
}
