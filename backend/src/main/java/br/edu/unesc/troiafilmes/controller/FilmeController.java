package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.request.FilmeRequest;
import br.edu.unesc.troiafilmes.dto.response.AvaliacaoResponse;
import br.edu.unesc.troiafilmes.dto.response.FilmeResponse;
import br.edu.unesc.troiafilmes.dto.response.PaginaResponse;
import br.edu.unesc.troiafilmes.service.AvaliacaoService;
import br.edu.unesc.troiafilmes.service.FilmeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Catálogo de filmes. Leitura para qualquer autenticado, escrita só para ADMIN.
 */
@RestController
@RequestMapping("/api/filmes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "4. Filmes", description = "Catálogo, busca e administração dos títulos")
public class FilmeController {

    private final FilmeService filmeService;
    private final AvaliacaoService avaliacaoService;

    @GetMapping
    @Operation(
            summary = "Busca paginada no catálogo",
            description = """
                    Os três filtros são opcionais e combináveis. Sem nenhum deles, devolve o
                    catálogo inteiro paginado. Cada item já traz as categorias e o resumo das
                    avaliações (nota média e total).
                    """)
    @ApiResponse(responseCode = "200", description = "Página de filmes")
    public PaginaResponse<FilmeResponse> buscar(
            @Parameter(description = "Parte do título, sem diferenciar maiúsculas", example = "sinal")
            @RequestParam(required = false) String titulo,

            @Parameter(description = "Id da categoria", example = "6")
            @RequestParam(required = false) Long categoriaId,

            @Parameter(description = "Ano de lançamento exato", example = "2023")
            @RequestParam(required = false) Integer ano,

            @PageableDefault(size = 12, sort = "titulo", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return filmeService.buscar(titulo, categoriaId, ano, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um filme com categorias e nota média")
    @ApiResponse(responseCode = "404", description = "Filme inexistente")
    public FilmeResponse buscarPorId(@PathVariable Long id) {
        return filmeService.buscarPorId(id);
    }

    @GetMapping("/{id}/avaliacoes")
    @Operation(summary = "Lista as avaliações recebidas por um filme")
    public PaginaResponse<AvaliacaoResponse> listarAvaliacoes(
            @PathVariable Long id,
            @PageableDefault(size = 10) Pageable pageable) {
        return avaliacaoService.listarDoFilme(id, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra um filme no catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Filme cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "403", description = "Requer papel ADMIN"),
            @ApiResponse(responseCode = "404", description = "Alguma categoria informada não existe"),
            @ApiResponse(responseCode = "409", description = "Título já cadastrado nesse ano")
    })
    public FilmeResponse criar(@Valid @RequestBody FilmeRequest request) {
        return filmeService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Atualiza um filme",
            description = "As categorias enviadas substituem integralmente as anteriores.")
    public FilmeResponse atualizar(@PathVariable Long id,
                                   @Valid @RequestBody FilmeRequest request) {
        return filmeService.atualizar(id, request);
    }

    @PostMapping(path = "/{id}/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Envia a imagem de pôster do filme",
            description = """
                    Recebe o arquivo no campo `imagem` de um formulário multipart.
                    São aceitos JPEG, PNG e WebP de até 2 MB.

                    O formato é reconhecido pelos bytes iniciais do arquivo, e não pelo
                    `Content-Type` informado nem pela extensão do nome — os dois são
                    escolhidos por quem envia. O nome original é descartado: o arquivo é
                    gravado com um UUID.

                    A imagem gravada é devolvida em `posterUrl` e fica acessível
                    publicamente, porque a tag `img` do navegador não envia o cabeçalho
                    Authorization.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pôster atualizado"),
            @ApiResponse(responseCode = "400", description = "Arquivo ausente, vazio ou que não é imagem"),
            @ApiResponse(responseCode = "403", description = "Requer papel ADMIN"),
            @ApiResponse(responseCode = "404", description = "Filme inexistente"),
            @ApiResponse(responseCode = "413", description = "Imagem acima do limite de tamanho")
    })
    public FilmeResponse enviarPoster(@PathVariable Long id,
                                      @RequestParam("imagem") MultipartFile imagem) {
        return filmeService.definirPoster(id, imagem);
    }

    @DeleteMapping("/{id}/poster")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Remove a imagem de pôster do filme",
            description = "O arquivo é apagado do disco e o filme volta a ser exibido "
                    + "com o cartão de degradê gerado a partir do título.")
    @ApiResponse(responseCode = "200", description = "Pôster removido")
    public FilmeResponse removerPoster(@PathVariable Long id) {
        return filmeService.removerPoster(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Remove um filme do catálogo",
            description = "As avaliações, itens de lista e registros de histórico do filme "
                    + "são removidos junto, por cascata.")
    @ApiResponse(responseCode = "204", description = "Filme removido")
    public void remover(@PathVariable Long id) {
        filmeService.remover(id);
    }
}
