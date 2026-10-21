package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.config.SecurityConfig;
import br.edu.unesc.troiafilmes.dto.request.FilmeRequest;
import br.edu.unesc.troiafilmes.dto.response.FilmeResponse;
import br.edu.unesc.troiafilmes.dto.response.PaginaResponse;
import br.edu.unesc.troiafilmes.exception.GlobalExceptionHandler;
import br.edu.unesc.troiafilmes.config.RespostaJsonDeSeguranca;
import br.edu.unesc.troiafilmes.config.SecurityFilter;
import br.edu.unesc.troiafilmes.config.TokenConfig;
import br.edu.unesc.troiafilmes.repository.UsuarioRepository;
import br.edu.unesc.troiafilmes.service.AvaliacaoService;
import br.edu.unesc.troiafilmes.service.FilmeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica a camada web do catálogo: quem alcança cada rota e como os erros
 * de validação saem na resposta.
 *
 * <p>A configuração de segurança real é carregada, então a autorização
 * testada aqui é a mesma que roda em produção.</p>
 */
@WebMvcTest(FilmeController.class)
@Import({SecurityConfig.class, SecurityFilter.class,
        RespostaJsonDeSeguranca.class, GlobalExceptionHandler.class})
@DisplayName("FilmeController")
class FilmeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FilmeService filmeService;

    @MockitoBean
    private AvaliacaoService avaliacaoService;

    // Dependências do filtro do token: o filtro em si é o real, para não furar a cadeia.
    @MockitoBean
    private TokenConfig tokenConfig;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("nega a listagem do catálogo sem autenticação")
    void semTokenRecebe401() throws Exception {
        mockMvc.perform(get("/api/filmes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("Autenticação necessária")));

        verifyNoInteractions(filmeService);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("permite a listagem do catálogo para USER")
    void usuarioComumListaCatalogo() throws Exception {
        when(filmeService.buscar(any(), any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(), 0, 12, 0, 0, true));

        mockMvc.perform(get("/api/filmes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("nega o cadastro de filme para USER")
    void usuarioComumNaoCadastraFilme() throws Exception {
        mockMvc.perform(post("/api/filmes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filmeValido())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(filmeService, never()).criar(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("permite o cadastro de filme para ADMIN")
    void administradorCadastraFilme() throws Exception {
        when(filmeService.criar(any(FilmeRequest.class))).thenReturn(new FilmeResponse(
                1L, "Interestelar", "Sinopse.", 2014, 169, null, null, List.of(), null, 0L));

        mockMvc.perform(post("/api/filmes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filmeValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Interestelar"));

        verify(filmeService).criar(any(FilmeRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("devolve 400 com a lista de campos inválidos")
    void devolveErrosDeValidacaoPorCampo() throws Exception {
        // Título em branco, ano anterior a 1888, duração negativa e sem categorias.
        FilmeRequest invalido = new FilmeRequest("", "Sinopse.", 1500, -10, null, Set.of());

        mockMvc.perform(post("/api/filmes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos").isArray())
                .andExpect(jsonPath("$.campos[?(@.campo == 'titulo')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'anoLancamento')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'duracaoMinutos')]").exists())
                .andExpect(jsonPath("$.campos[?(@.campo == 'categoriaIds')]").exists());

        verify(filmeService, never()).criar(any());
    }

    private static FilmeRequest filmeValido() {
        return new FilmeRequest("Interestelar", "Sinopse.", 2014, 169, null, Set.of(1L, 6L));
    }
}
