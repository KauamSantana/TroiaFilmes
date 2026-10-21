import { useState } from 'react'
import PainelFilmes from './admin/PainelFilmes'
import PainelCategorias from './admin/PainelCategorias'
import PainelPlanos from './admin/PainelPlanos'
import PainelUsuarios from './admin/PainelUsuarios'

const ABAS = [
  { id: 'filmes', rotulo: 'Filmes', componente: PainelFilmes },
  { id: 'categorias', rotulo: 'Categorias', componente: PainelCategorias },
  { id: 'planos', rotulo: 'Planos', componente: PainelPlanos },
  { id: 'usuarios', rotulo: 'Usuários', componente: PainelUsuarios },
]

/**
 * Area administrativa. A rota ja e protegida por <RotaAdmin>, mas quem
 * realmente decide e o backend: um USER que chegasse aqui receberia 403 em
 * toda operacao de escrita.
 */
export default function Admin() {
  const [abaAtiva, setAbaAtiva] = useState('filmes')
  const Painel = ABAS.find((aba) => aba.id === abaAtiva).componente

  return (
    <>
      <div className="titulo-secao">
        <div>
          <h2>Administração</h2>
          <p className="subtitulo">Gestão do catálogo, dos planos e das contas</p>
        </div>
      </div>

      <div className="abas">
        {ABAS.map((aba) => (
          <button
            key={aba.id}
            type="button"
            className={`aba ${abaAtiva === aba.id ? 'ativa' : ''}`}
            onClick={() => setAbaAtiva(aba.id)}
          >
            {aba.rotulo}
          </button>
        ))}
      </div>

      <Painel />
    </>
  )
}
