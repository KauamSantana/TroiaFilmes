import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// O proxy encaminha para o backend em 8080 durante o desenvolvimento,
// evitando configuracao extra de CORS no navegador.
//
// Alem da API, sao encaminhadas as duas origens de imagem:
//   /posters  posteres da carga inicial, que viajam dentro do JAR
//   /uploads  imagens enviadas pela administracao em tempo de execucao
// Assim o posterUrl gravado no banco pode ser relativo, sem host nem porta
// fixos, e o mesmo dado serve ao Vite aqui e ao backend em producao.
const DESTINO = 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: DESTINO, changeOrigin: true },
      '/posters': { target: DESTINO, changeOrigin: true },
      '/uploads': { target: DESTINO, changeOrigin: true },
    },
  },
})
