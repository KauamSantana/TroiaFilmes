import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Layout from './components/Layout'
import { RotaAdmin, RotaAutenticada, RotaComPerfil } from './components/RotaProtegida'

import Login from './pages/Login'
import Registro from './pages/Registro'
import SelecaoPerfil from './pages/SelecaoPerfil'
import Catalogo from './pages/Catalogo'
import DetalheFilme from './pages/DetalheFilme'
import MinhaLista from './pages/MinhaLista'
import ContinuarAssistindo from './pages/ContinuarAssistindo'
import Planos from './pages/Planos'
import Admin from './pages/Admin'

/**
 * Rotas do TroiaFilmes, em tres niveis de protecao:
 *   RotaAutenticada -> exige sessao
 *   RotaComPerfil   -> exige tambem um perfil selecionado
 *   RotaAdmin       -> exige papel ADMIN
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/registro" element={<Registro />} />

          <Route element={<RotaAutenticada />}>
            <Route path="/perfis" element={<SelecaoPerfil />} />

            <Route element={<RotaComPerfil />}>
              <Route element={<Layout />}>
                <Route path="/catalogo" element={<Catalogo />} />
                <Route path="/filmes/:id" element={<DetalheFilme />} />
                <Route path="/minha-lista" element={<MinhaLista />} />
                <Route path="/continuar-assistindo" element={<ContinuarAssistindo />} />
                <Route path="/planos" element={<Planos />} />

                <Route element={<RotaAdmin />}>
                  <Route path="/admin" element={<Admin />} />
                </Route>
              </Route>
            </Route>
          </Route>

          <Route path="/" element={<Navigate to="/catalogo" replace />} />
          <Route path="*" element={<Navigate to="/catalogo" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
