/** Faixa de mensagem: erro, sucesso ou informacao neutra. */
export default function Aviso({ tipo = 'neutro', children }) {
  if (!children) return null
  return <div className={`aviso ${tipo}`}>{children}</div>
}

export function Carregando({ texto = 'Carregando...' }) {
  return <div className="carregando">{texto}</div>
}

export function Vazio({ children }) {
  return <div className="vazio">{children}</div>
}
