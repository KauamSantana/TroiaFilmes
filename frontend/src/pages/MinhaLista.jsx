import { useEffect, useState } from 'react'
import { mensagemDeErro, minhaLista as apiLista } from '../services/api'
import { useAuth } from '../context/AuthContext'
import CartaoFilme from '../components/CartaoFilme'
import Aviso, { Carregando, Vazio } from '../components/Aviso'

export default function MinhaLista() {
  const { perfilAtivo } = useAuth()
  const [itens, setItens] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  useEffect(() => {
    apiLista
      .listar(perfilAtivo.id)
      .then(({ data }) => setItens(data))
      .catch((falha) => setErro(mensagemDeErro(falha, 'Não foi possível carregar a sua lista.')))
      .finally(() => setCarregando(false))
  }, [perfilAtivo.id])

  if (carregando) return <Carregando texto="Carregando a sua lista..." />

  return (
    <>
      <div className="titulo-secao">
        <div>
          <h2>Minha Lista</h2>
          <p className="subtitulo">
            Filmes salvos pelo perfil {perfilAtivo.nome} &middot; {itens.length} título(s)
          </p>
        </div>
      </div>

      <Aviso tipo="erro">{erro}</Aviso>

      {itens.length === 0 ? (
        <Vazio>
          Sua lista está vazia. Abra um filme no catálogo e use o botão
          &quot;+ Minha Lista&quot;.
        </Vazio>
      ) : (
        <div className="grade-filmes">
          {itens.map((item) => (
            <CartaoFilme key={item.id} filme={item.filme} />
          ))}
        </div>
      )}
    </>
  )
}
