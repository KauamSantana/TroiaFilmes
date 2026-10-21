import { useEffect, useState } from 'react'
import { mensagemDeErro, usuarios as apiUsuarios } from '../../services/api'
import { useAuth } from '../../context/AuthContext'
import Aviso, { Carregando } from '../../components/Aviso'

/**
 * Listagem das contas com as duas acoes administrativas: ativar/desativar
 * (soft delete) e promover/rebaixar o papel.
 */
export default function PainelUsuarios() {
  const { usuario: eu } = useAuth()
  const [pagina, setPagina] = useState(null)
  const [numeroPagina, setNumeroPagina] = useState(0)
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  const carregar = () => {
    setCarregando(true)
    apiUsuarios
      .listar({ page: numeroPagina, size: 15 })
      .then(({ data }) => setPagina(data))
      .catch((falha) => setErro(mensagemDeErro(falha)))
      .finally(() => setCarregando(false))
  }

  useEffect(carregar, [numeroPagina])

  const alternarSituacao = async (alvo) => {
    setErro('')
    setSucesso('')
    try {
      await apiUsuarios.alterarSituacao(alvo.id, !alvo.ativo)
      setSucesso(`Conta ${alvo.email} ${alvo.ativo ? 'desativada' : 'reativada'}.`)
      carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
    }
  }

  const alternarPapel = async (alvo) => {
    const novoPapel = alvo.role === 'ADMIN' ? 'USER' : 'ADMIN'
    if (!window.confirm(`Alterar ${alvo.email} para ${novoPapel}?`)) return

    setErro('')
    setSucesso('')
    try {
      await apiUsuarios.alterarPapel(alvo.id, novoPapel)
      setSucesso(`Papel de ${alvo.email} alterado para ${novoPapel}.`)
      carregar()
    } catch (falha) {
      setErro(mensagemDeErro(falha))
    }
  }

  if (carregando) return <Carregando />

  return (
    <>
      <Aviso tipo="erro">{erro}</Aviso>
      <Aviso tipo="sucesso">{sucesso}</Aviso>

      <p className="subtitulo">{pagina?.totalElementos ?? 0} conta(s) cadastrada(s)</p>

      <div className="tabela-container">
        <table>
          <thead>
            <tr>
              <th>Id</th>
              <th>Nome</th>
              <th>E-mail</th>
              <th>Papel</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {pagina?.conteudo?.map((usuario) => {
              const souEu = usuario.id === eu.id
              return (
                <tr key={usuario.id}>
                  <td>{usuario.id}</td>
                  <td>
                    {usuario.nome} {souEu && <span className="etiqueta">você</span>}
                  </td>
                  <td>{usuario.email}</td>
                  <td>{usuario.role}</td>
                  <td>{usuario.ativo ? 'Ativa' : 'Desativada'}</td>
                  <td>
                    <div className="acoes-linha">
                      <button
                        type="button"
                        className="botao secundario pequeno"
                        onClick={() => alternarPapel(usuario)}
                        disabled={souEu}
                        title={souEu ? 'Você não pode alterar o próprio papel' : undefined}
                      >
                        {usuario.role === 'ADMIN' ? 'Tornar USER' : 'Tornar ADMIN'}
                      </button>
                      <button
                        type="button"
                        className={usuario.ativo ? 'botao perigo pequeno' : 'botao pequeno'}
                        onClick={() => alternarSituacao(usuario)}
                        disabled={souEu}
                        title={souEu ? 'Você não pode desativar a própria conta' : undefined}
                      >
                        {usuario.ativo ? 'Desativar' : 'Reativar'}
                      </button>
                    </div>
                  </td>
                </tr>
              )
            })}
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
  )
}
