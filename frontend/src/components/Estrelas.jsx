/** Representacao visual de uma nota de 1 a 5. */
export default function Estrelas({ nota, aoSelecionar }) {
  const valores = [1, 2, 3, 4, 5]

  if (!aoSelecionar) {
    return (
      <span className="estrelas" title={`Nota ${nota} de 5`}>
        {valores.map((v) => (v <= nota ? '★' : '☆')).join('')}
      </span>
    )
  }

  return (
    <span className="estrelas">
      {valores.map((valor) => (
        <button
          key={valor}
          type="button"
          onClick={() => aoSelecionar(valor)}
          aria-label={`Dar nota ${valor}`}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: 'inherit',
            font: 'inherit',
            fontSize: '1.4rem',
            padding: '0 1px',
          }}
        >
          {valor <= nota ? '★' : '☆'}
        </button>
      ))}
    </span>
  )
}
