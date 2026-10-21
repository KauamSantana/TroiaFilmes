import { Link } from 'react-router-dom'
import Poster from './Poster'

/** Cartao do filme usado nas grades de catalogo, lista e historico. */
export default function CartaoFilme({ filme, progresso }) {
  return (
    <Link to={`/filmes/${filme.id}`} className="cartao-filme">
      <Poster titulo={filme.titulo} url={filme.posterUrl} notaMedia={filme.notaMedia} />
      <div className="info-filme">
        <h3>{filme.titulo}</h3>
        <div className="meta-filme">
          {filme.anoLancamento} &middot; {filme.duracaoMinutos} min
          {filme.totalAvaliacoes > 0 && ` · ${filme.totalAvaliacoes} avaliações`}
        </div>
        {typeof progresso === 'number' && (
          <div className="barra-progresso" title={`${progresso}% assistido`}>
            <span style={{ width: `${progresso}%` }} />
          </div>
        )}
      </div>
    </Link>
  )
}
