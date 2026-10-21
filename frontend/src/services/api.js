import axios from 'axios'

const CHAVE_ACCESS = 'troiafilmes.token'
const CHAVE_REFRESH = 'troiafilmes.refreshToken'
const CHAVE_USUARIO = 'troiafilmes.usuario'
const CHAVE_PERFIL = 'troiafilmes.perfilId'

export const armazenamento = {
  getAccessToken: () => localStorage.getItem(CHAVE_ACCESS),
  getRefreshToken: () => localStorage.getItem(CHAVE_REFRESH),

  getUsuario() {
    const bruto = localStorage.getItem(CHAVE_USUARIO)
    return bruto ? JSON.parse(bruto) : null
  },

  getPerfilId: () => localStorage.getItem(CHAVE_PERFIL),
  setPerfilId: (id) => localStorage.setItem(CHAVE_PERFIL, String(id)),

  // Grava a resposta do login (e da renovacao): { token, refreshToken, usuario }.
  salvarSessao({ token, refreshToken, usuario }) {
    localStorage.setItem(CHAVE_ACCESS, token)
    localStorage.setItem(CHAVE_REFRESH, refreshToken)
    localStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario))
  },

  limpar() {
    ;[CHAVE_ACCESS, CHAVE_REFRESH, CHAVE_USUARIO, CHAVE_PERFIL].forEach((chave) =>
      localStorage.removeItem(chave),
    )
  },
}

export const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Anexa o JWT em toda requisicao que sair daqui.
api.interceptors.request.use((config) => {
  const token = armazenamento.getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Rotas de autenticacao nao devem disparar a renovacao automatica.
const ROTAS_DE_AUTENTICACAO = ['/auth/login', '/auth/registrar', '/auth/refresh']

let renovacaoEmAndamento = null

/**
 * Renova o access token uma unica vez, mesmo que varias requisicoes recebam
 * 401 ao mesmo tempo: todas aguardam a mesma promessa.
 */
function renovarToken() {
  if (!renovacaoEmAndamento) {
    const refreshToken = armazenamento.getRefreshToken()

    renovacaoEmAndamento = axios
      .post('/api/auth/refresh', { refreshToken })
      .then(({ data }) => {
        armazenamento.salvarSessao(data)
        return data.token
      })
      .finally(() => {
        renovacaoEmAndamento = null
      })
  }
  return renovacaoEmAndamento
}

api.interceptors.response.use(
  (resposta) => resposta,
  async (erro) => {
    const requisicao = erro.config
    const ehAuth = ROTAS_DE_AUTENTICACAO.some((rota) => requisicao?.url?.includes(rota))

    if (
      erro.response?.status === 401 &&
      !requisicao._jaTentouRenovar &&
      !ehAuth &&
      armazenamento.getRefreshToken()
    ) {
      requisicao._jaTentouRenovar = true
      try {
        const novoToken = await renovarToken()
        requisicao.headers.Authorization = `Bearer ${novoToken}`
        return api(requisicao)
      } catch {
        // Refresh token tambem invalido: a sessao acabou de vez.
        armazenamento.limpar()
        window.location.href = '/login'
      }
    }

    return Promise.reject(erro)
  },
)

/**
 * Extrai a mensagem do corpo de erro padronizado da API.
 * Quando ha erros de validacao por campo, junta todos em uma linha.
 */
export function mensagemDeErro(erro, padrao = 'Nao foi possivel completar a operacao.') {
  const dados = erro?.response?.data
  if (!dados) return erro?.message || padrao

  if (Array.isArray(dados.campos) && dados.campos.length > 0) {
    return dados.campos.map((campo) => `${campo.campo}: ${campo.mensagem}`).join(' | ')
  }
  return dados.mensagem || padrao
}

// --------------------------------------------------------------- endpoints

export const autenticacao = {
  login: (email, senha) => api.post('/auth/login', { email, senha }),
  registrar: (nome, email, senha) => api.post('/auth/registrar', { nome, email, senha }),
  logout: () => api.post('/auth/logout'),
}

export const filmes = {
  buscar: (params) => api.get('/filmes', { params }),
  porId: (id) => api.get(`/filmes/${id}`),
  avaliacoes: (id) => api.get(`/filmes/${id}/avaliacoes`),
  criar: (dados) => api.post('/filmes', dados),
  atualizar: (id, dados) => api.put(`/filmes/${id}`, dados),
  remover: (id) => api.delete(`/filmes/${id}`),

  /**
   * Envia a imagem de poster como multipart.
   *
   * O Content-Type e deixado em branco de proposito: o navegador precisa
   * gera-lo sozinho para incluir o "boundary" que separa as partes do
   * formulario. Forcando o application/json padrao da instancia, o Spring
   * nao consegue ler o arquivo.
   */
  enviarPoster: (id, arquivo) => {
    const dados = new FormData()
    dados.append('imagem', arquivo)
    return api.post(`/filmes/${id}/poster`, dados, {
      headers: { 'Content-Type': undefined },
    })
  },

  removerPoster: (id) => api.delete(`/filmes/${id}/poster`),
}

export const categorias = {
  listar: () => api.get('/categorias'),
  criar: (dados) => api.post('/categorias', dados),
  atualizar: (id, dados) => api.put(`/categorias/${id}`, dados),
  remover: (id) => api.delete(`/categorias/${id}`),
}

export const perfis = {
  listar: () => api.get('/perfis'),
  criar: (dados) => api.post('/perfis', dados),
  atualizar: (id, dados) => api.put(`/perfis/${id}`, dados),
  remover: (id) => api.delete(`/perfis/${id}`),
}

export const avaliacoes = {
  doPerfil: (perfilId) => api.get(`/perfis/${perfilId}/avaliacoes`),
  criar: (perfilId, dados) => api.post(`/perfis/${perfilId}/avaliacoes`, dados),
  atualizar: (id, dados) => api.put(`/avaliacoes/${id}`, dados),
  remover: (id) => api.delete(`/avaliacoes/${id}`),
}

export const minhaLista = {
  listar: (perfilId) => api.get(`/perfis/${perfilId}/lista`),
  contem: (perfilId, filmeId) => api.get(`/perfis/${perfilId}/lista/${filmeId}`),
  adicionar: (perfilId, filmeId) => api.post(`/perfis/${perfilId}/lista/${filmeId}`),
  remover: (perfilId, filmeId) => api.delete(`/perfis/${perfilId}/lista/${filmeId}`),
}

export const historico = {
  listar: (perfilId) => api.get(`/perfis/${perfilId}/historico`),
  continuarAssistindo: (perfilId) => api.get(`/perfis/${perfilId}/continuar-assistindo`),
  registrarProgresso: (perfilId, dados) => api.post(`/perfis/${perfilId}/historico`, dados),
  remover: (perfilId, filmeId) => api.delete(`/perfis/${perfilId}/historico/${filmeId}`),
}

export const planos = {
  listar: (apenasAtivos = true) => api.get('/planos', { params: { apenasAtivos } }),
  criar: (dados) => api.post('/planos', dados),
  atualizar: (id, dados) => api.put(`/planos/${id}`, dados),
  remover: (id) => api.delete(`/planos/${id}`),
}

export const assinaturas = {
  ativa: () => api.get('/assinaturas/ativa'),
  historico: () => api.get('/assinaturas/historico'),
  contratar: (planoId) => api.post('/assinaturas', { planoId }),
  cancelar: () => api.delete('/assinaturas'),
}

export const usuarios = {
  listar: (params) => api.get('/usuarios', { params }),
  alterarSituacao: (id, ativo) => api.patch(`/usuarios/${id}/situacao`, null, { params: { ativo } }),
  alterarPapel: (id, role) => api.patch(`/usuarios/${id}/papel`, null, { params: { role } }),
}

export const conta = {
  buscar: () => api.get('/conta'),
  atualizar: (dados) => api.put('/conta', dados),
}
