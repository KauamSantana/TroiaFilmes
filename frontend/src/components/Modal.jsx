import { useEffect } from 'react'

/** Janela modal simples, fechavel pelo Esc ou por clique no fundo. */
export default function Modal({ titulo, aoFechar, children }) {
  useEffect(() => {
    const aoTeclar = (evento) => {
      if (evento.key === 'Escape') aoFechar()
    }
    document.addEventListener('keydown', aoTeclar)
    return () => document.removeEventListener('keydown', aoTeclar)
  }, [aoFechar])

  return (
    <div className="fundo-modal" onClick={aoFechar}>
      <div className="modal" onClick={(evento) => evento.stopPropagation()}>
        <div className="modal-cabecalho">
          <h2>{titulo}</h2>
          <button type="button" className="botao secundario pequeno" onClick={aoFechar}>
            Fechar
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
