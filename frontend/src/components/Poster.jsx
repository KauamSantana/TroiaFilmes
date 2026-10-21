import { useState } from 'react'

/**
 * Poster do filme, com degrade e titulo como base.
 *
 * O titulo fica SEMPRE renderizado, e a imagem remota entra por cima quando
 * carrega. Isso resolve dois casos de uma vez: enquanto a imagem esta
 * baixando (internet lenta na apresentacao, por exemplo) e quando ela nunca
 * chega (offline, link quebrado), o cartao continua legivel em vez de ficar
 * um retangulo vazio. A cor do degrade vem do proprio titulo, entao cada
 * filme fica visualmente distinto sem depender da rede.
 */
export default function Poster({ titulo, url, notaMedia, className = '' }) {
  const [falhou, setFalhou] = useState(false)
  const mostrarImagem = url && !falhou

  return (
    <div className={`poster ${className}`} style={{ background: degradeDe(titulo) }}>
      <span>{titulo}</span>
      {mostrarImagem && (
        <img src={url} alt={`Pôster de ${titulo}`} loading="lazy" onError={() => setFalhou(true)} />
      )}
      {notaMedia ? <span className="selo-nota">★ {notaMedia.toFixed(1)}</span> : null}
    </div>
  )
}

/** Cor estavel por titulo: o mesmo filme sempre recebe o mesmo degrade. */
function degradeDe(titulo = '') {
  let hash = 0
  for (let i = 0; i < titulo.length; i += 1) {
    hash = titulo.charCodeAt(i) + ((hash << 5) - hash)
  }
  const matiz = Math.abs(hash) % 360
  return `linear-gradient(145deg, hsl(${matiz} 38% 18%), hsl(${(matiz + 40) % 360} 44% 11%))`
}
