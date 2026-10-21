import { useEffect, useState } from 'react'
import { assinaturas as apiAssinaturas, mensagemDeErro, planos as apiPlanos } from '../services/api'
import Aviso, { Carregando } from '../components/Aviso'

/**
 * Contratacao de plano. E aqui que a regra de uma unica assinatura ativa
 * fica visivel: trocar de plano cancela a anterior automaticamente, e tentar
 * migrar para um plano menor do que os perfis em uso devolve 409.
 */
export default function Planos() {
  const [lista, setLista] = useState([])
  const [vigente, setVigente] = useState(null)
  const [historico, setHistorico] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  const carregar = async () => {
    setCarregando(true)
    try {
      const [planosData, ativa, historicoData] = await Promise.all([
        apiPlanos.listar(true),
        apiAssinaturas.ativa().catch(() => null),
        apiAssinaturas.historico().catch(() => ({ data: [] })),
      ])
      setLista(planosData.data)
      setVigente(ativa?.data ?? null)
      setHistorico(historicoData.data)
    } catch (falha) {
      setErro(mensagemDeErro(falha, 'Não foi possível carregar os planos.'))
    } finally {
      setCarregando(false)
    }
  }

  useEffect(() => {
    carregar()
  }, [])

  const contratar = async (plano) => {
    setErro('')
    setSucesso('')
    try {
      await apiAssinaturas.contratar(plano.id)
      setSucesso(`Plano ${plano.nome} contratado. A assinatura anterior foi cancelada.`)
      await carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
    }
  }

  const cancelar = async () => {
    if (!window.confirm('Cancelar a assinatura vigente?')) return
    setErro('')
    setSucesso('')
    try {
      await apiAssinaturas.cancelar()
      setSucesso('Assinatura cancelada. A conta fica limitada a 1 perfil.')
      await carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
    }
  }

  if (carregando) return <Carregando texto="Carregando planos..." />

  return (
    <>
      <div className="titulo-secao">
        <div>
          <h2>Planos</h2>
          <p className="subtitulo">
            {vigente
              ? `Plano vigente: ${vigente.plano.nome} · até ${vigente.plano.maxPerfis} perfil(is) · ${vigente.plano.qualidadeMaxima}`
              : 'Você não possui assinatura ativa.'}
          </p>
        </div>
        {vigente && (
          <button type="button" className="botao perigo" onClick={cancelar}>
            Cancelar assinatura
          </button>
        )}
      </div>

      <Aviso tipo="erro">{erro}</Aviso>
      <Aviso tipo="sucesso">{sucesso}</Aviso>

      <div className="grade-planos">
        {lista.map((plano) => {
          const ehVigente = vigente?.plano?.id === plano.id
          return (
            <div key={plano.id} className={`cartao-plano ${ehVigente ? 'vigente' : ''}`}>
              <h3>{plano.nome}</h3>
              <div className="preco">
                R$ {Number(plano.preco).toFixed(2).replace('.', ',')}
                <small> /mês</small>
              </div>
              <ul>
                <li>
                  Até {plano.maxPerfis} perfil{plano.maxPerfis > 1 ? 'is' : ''}
                </li>
                <li>Qualidade {plano.qualidadeMaxima}</li>
                {plano.descricao && <li>{plano.descricao}</li>}
              </ul>
              <button
                type="button"
                className={ehVigente ? 'botao secundario' : 'botao'}
                onClick={() => contratar(plano)}
                disabled={ehVigente}
              >
                {ehVigente ? 'Plano atual' : 'Contratar'}
              </button>
            </div>
          )
        })}
      </div>

      {historico.length > 0 && (
        <div className="bloco">
          <h2>Histórico de assinaturas</h2>
          <div className="tabela-container">
            <table>
              <thead>
                <tr>
                  <th>Plano</th>
                  <th>Início</th>
                  <th>Fim</th>
                  <th>Situação</th>
                </tr>
              </thead>
              <tbody>
                {historico.map((assinatura) => (
                  <tr key={assinatura.id}>
                    <td>{assinatura.plano.nome}</td>
                    <td>{formatarData(assinatura.dataInicio)}</td>
                    <td>{assinatura.dataFim ? formatarData(assinatura.dataFim) : '—'}</td>
                    <td>{assinatura.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </>
  )
}

function formatarData(iso) {
  if (!iso) return '—'
  const [ano, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${ano}`
}
