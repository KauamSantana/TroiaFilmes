import { useEffect, useState } from 'react'
import { historico as apiHistorico, mensagemDeErro } from '../services/api'
import { useAuth } from '../context/AuthContext'
import CartaoFilme from '../components/CartaoFilme'
import Aviso, { Carregando, Vazio } from '../components/Aviso'

/**
 * Mostra em duas secoes o que veio do historico: o que esta em andamento
 * (percentual abaixo de 90%) e o que ja foi concluido.
 */
export default function ContinuarAssistindo() {
  const { perfilAtivo } = useAuth()
  const [emAndamento, setEmAndamento] = useState([])
  const [tudo, setTudo] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  useEffect(() => {
    Promise.all([
      apiHistorico.continuarAssistindo(perfilAtivo.id),
      apiHistorico.listar(perfilAtivo.id),
    ])
      .then(([andamento, completo]) => {
        setEmAndamento(andamento.data)
        setTudo(completo.data)
      })
      .catch((falha) => setErro(mensagemDeErro(falha, 'Não foi possível carregar o histórico.')))
      .finally(() => setCarregando(false))
  }, [perfilAtivo.id])

  const concluidos = tudo.filter((registro) => registro.concluido)

  if (carregando) return <Carregando texto="Carregando o seu histórico..." />

  return (
    <>
      <div className="titulo-secao">
        <div>
          <h2>Continuar assistindo</h2>
          <p className="subtitulo">Histórico do perfil {perfilAtivo.nome}</p>
        </div>
      </div>

      <Aviso tipo="erro">{erro}</Aviso>

      {emAndamento.length === 0 ? (
        <Vazio>
          Nada em andamento. Abra um filme, clique em Assistir e saia antes do fim: ele
          aparece aqui para você continuar de onde parou.
        </Vazio>
      ) : (
        <div className="grade-filmes">
          {emAndamento.map((registro) => (
            <CartaoFilme
              key={registro.id}
              filme={registro.filme}
              progresso={registro.percentualAssistido}
              destino={`/filmes/${registro.filme.id}?assistir=1`}
            />
          ))}
        </div>
      )}

      {concluidos.length > 0 && (
        <>
          <div className="titulo-secao" style={{ marginTop: '2.5rem' }}>
            <div>
              <h2>Já assistidos</h2>
              <p className="subtitulo">{concluidos.length} título(s) concluído(s)</p>
            </div>
          </div>
          <div className="grade-filmes">
            {concluidos.map((registro) => (
              <CartaoFilme key={registro.id} filme={registro.filme} progresso={100} />
            ))}
          </div>
        </>
      )}
    </>
  )
}
