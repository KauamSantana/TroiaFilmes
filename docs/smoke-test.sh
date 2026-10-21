#!/usr/bin/env bash
# =====================================================================
# TroiaFilmes - roteiro de verificacao da API
#
# Exercita o fluxo completo e confere o status HTTP de cada passo,
# inclusive os casos de erro que provam as regras de negocio.
#
# Nenhum id e fixo no script: todos sao descobertos pela propria API.
# Isso importa porque os ids do seed nao sao estaveis (uma migration
# revertida, por exemplo, consome numeros da sequence sem devolve-los).
#
# Uso:  bash docs/smoke-test.sh [http://localhost:8080]
# =====================================================================
set -u

BASE="${1:-http://localhost:8080}"
PASSOU=0
FALHOU=0

verde()    { printf '\033[32m%s\033[0m\n' "$1"; }
vermelho() { printf '\033[31m%s\033[0m\n' "$1"; }
titulo()   { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }
detalhe()  { printf '        %s\n' "$1"; }

# conferir <descricao> <status-esperado> <status-recebido>
conferir() {
  if [ "$2" = "$3" ]; then
    verde   "  OK    $1 (HTTP $3)"
    PASSOU=$((PASSOU + 1))
  else
    vermelho "  FALHA $1 (esperado $2, recebeu $3)"
    FALHOU=$((FALHOU + 1))
  fi
}

# chamar <metodo> <caminho> <token|-> <corpo|-> -> imprime "<corpo>\n<status>"
chamar() {
  local metodo="$1" caminho="$2" token="$3" corpo="$4"
  local args=(-s -w '\n%{http_code}' -X "$metodo" "$BASE$caminho" -H 'Content-Type: application/json')
  [ "$token" != "-" ] && args+=(-H "Authorization: Bearer $token")
  [ "$corpo" != "-" ] && args+=(-d "$corpo")
  curl "${args[@]}"
}

status() { printf '%s' "$1" | tail -n1; }
corpo()  { printf '%s' "$1" | sed '$d'; }

# ------------------------------------------------- extracao de JSON sem jq
# A API responde em JSON compacto, entao quebrar em '{' isola um objeto
# por linha e permite casar campos irmaos.

# texto <json> <campo>
texto() {
  printf '%s' "$1" | grep -o "\"$2\":\"[^\"]*\"" | head -1 | sed 's/.*:"//;s/"$//'
}

# numero <json> <campo>
numero() {
  printf '%s' "$1" | grep -o "\"$2\":[0-9]*" | head -1 | grep -o '[0-9]*$'
}

# id_por_nome <json-de-lista> <nome> -> id do objeto cujo "nome" bate
id_por_nome() {
  printf '%s' "$1" | tr '{' '\n' \
    | grep -F "\"nome\":\"$2\"" \
    | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*$'
}

exigir() {
  if [ -z "${2:-}" ]; then
    vermelho "  ABORTADO: nao foi possivel descobrir $1. A API respondeu como esperado?"
    exit 1
  fi
}

titulo "1. Rotas publicas e protecao por autenticacao"

r=$(chamar GET /api/filmes - -)
conferir "catalogo sem token e recusado" 401 "$(status "$r")"

r=$(chamar GET /v3/api-docs - -)
conferir "documentacao OpenAPI e publica" 200 "$(status "$r")"

titulo "2. Autenticacao"

r=$(chamar POST /api/auth/login - '{"email":"admin@troiafilmes.com","senha":"Admin@123"}')
conferir "login do ADMIN" 200 "$(status "$r")"
TOKEN_ADMIN=$(texto "$(corpo "$r")" token)
exigir "o token do ADMIN" "$TOKEN_ADMIN"

r=$(chamar POST /api/auth/login - '{"email":"usuario@troiafilmes.com","senha":"User@123"}')
conferir "login do USER" 200 "$(status "$r")"
CORPO_USER=$(corpo "$r")
TOKEN_USER=$(texto "$CORPO_USER" token)
REFRESH_USER=$(texto "$CORPO_USER" refreshToken)
exigir "o token do USER" "$TOKEN_USER"

r=$(chamar POST /api/auth/login - '{"email":"usuario@troiafilmes.com","senha":"senhaErrada"}')
conferir "senha incorreta e recusada" 401 "$(status "$r")"

r=$(chamar POST /api/auth/login - '{"email":"nao-e-email","senha":"x"}')
conferir "e-mail mal formado e recusado na validacao" 400 "$(status "$r")"

titulo "2b. Cadastro de conta"

# E-mail unico a cada execucao, para o roteiro poder rodar varias vezes.
EMAIL_NOVO="smoke.$(date +%s)@exemplo.com"
r=$(chamar POST /api/auth/registrar - "{\"nome\":\"Conta do Smoke Test\",\"email\":\"$EMAIL_NOVO\",\"senha\":\"Senha@123\"}")
conferir "cadastro de conta nova" 201 "$(status "$r")"

EMAIL_MAIUSCULO=$(printf '%s' "$EMAIL_NOVO" | tr 'a-z' 'A-Z')
r=$(chamar POST /api/auth/registrar - "{\"nome\":\"Copia\",\"email\":\"$EMAIL_MAIUSCULO\",\"senha\":\"Senha@123\"}")
conferir "mesmo e-mail em maiusculas e recusado" 409 "$(status "$r")"

r=$(chamar POST /api/auth/registrar - '{"nome":"Ab","email":"nao-e-email","senha":"123"}')
conferir "cadastro com campos invalidos e recusado" 400 "$(status "$r")"

r=$(chamar POST /api/auth/login - "{\"email\":\"$EMAIL_NOVO\",\"senha\":\"Senha@123\"}")
conferir "conta nova faz login" 200 "$(status "$r")"
TOKEN_NOVO=$(texto "$(corpo "$r")" token)
exigir "o token da conta nova" "$TOKEN_NOVO"

r=$(chamar GET /api/perfis "$TOKEN_NOVO" -)
PERFIL_PRINCIPAL=$(id_por_nome "$(corpo "$r")" "Principal")
if [ -n "$PERFIL_PRINCIPAL" ]; then conferir "conta nova ja nasce com o perfil Principal" ok ok
else conferir "conta nova ja nasce com o perfil Principal" "perfil Principal" "nenhum"; fi

r=$(chamar GET /api/assinaturas/ativa "$TOKEN_NOVO" -)
conferir "conta nova ja nasce com assinatura ativa" 200 "$(status "$r")"
case "$(corpo "$r")" in
  *'"Básico"'*) conferir "a assinatura inicial e do plano Basico" ok ok ;;
  *)            conferir "a assinatura inicial e do plano Basico" "Basico" "outro plano" ;;
esac

titulo "3. Descoberta dos ids (nada fixo no script)"

r=$(chamar GET /api/categorias "$TOKEN_ADMIN" -)
conferir "lista as categorias" 200 "$(status "$r")"
CATEGORIAS=$(corpo "$r")
ID_CATEGORIA=$(id_por_nome "$CATEGORIAS" "Ação")
exigir "o id da categoria Acao" "$ID_CATEGORIA"
detalhe "categoria 'Ação' = id $ID_CATEGORIA"

r=$(chamar GET /api/planos "$TOKEN_USER" -)
conferir "lista os planos" 200 "$(status "$r")"
PLANOS=$(corpo "$r")
ID_PLANO_PADRAO=$(id_por_nome "$PLANOS" "Padrão")
ID_PLANO_PREMIUM=$(id_por_nome "$PLANOS" "Premium")
exigir "o id do plano Padrao" "$ID_PLANO_PADRAO"
exigir "o id do plano Premium" "$ID_PLANO_PREMIUM"
detalhe "planos: Padrão = $ID_PLANO_PADRAO, Premium = $ID_PLANO_PREMIUM"

r=$(chamar GET /api/perfis "$TOKEN_USER" -)
conferir "lista os perfis da conta" 200 "$(status "$r")"
ID_PERFIL=$(id_por_nome "$(corpo "$r")" "Principal")
exigir "o id do perfil Principal" "$ID_PERFIL"
detalhe "perfil 'Principal' = id $ID_PERFIL"

titulo "4. Autorizacao por papel"

r=$(chamar GET /api/filmes "$TOKEN_USER" -)
conferir "USER consulta o catalogo" 200 "$(status "$r")"

NOVO_FILME="{\"titulo\":\"Filme do Smoke Test\",\"sinopse\":\"Criado pelo roteiro de verificacao.\",\"anoLancamento\":2026,\"duracaoMinutos\":100,\"categoriaIds\":[$ID_CATEGORIA]}"

# Uma execucao interrompida no meio nao chega a limpar o filme de teste, e
# a proxima esbarraria na regra de titulo duplicado. Removemos o resto antes
# de comecar, para que o roteiro possa ser rodado quantas vezes quiser.
r=$(chamar GET "/api/filmes?titulo=Filme%20do%20Smoke%20Test" "$TOKEN_ADMIN" -)
ID_ANTIGO=$(numero "$(corpo "$r")" id)
if [ -n "$ID_ANTIGO" ]; then
  chamar DELETE "/api/filmes/$ID_ANTIGO" "$TOKEN_ADMIN" - > /dev/null
  detalhe "removido o filme de teste deixado por uma execucao anterior (id $ID_ANTIGO)"
fi

r=$(chamar POST /api/filmes "$TOKEN_USER" "$NOVO_FILME")
conferir "USER nao cadastra filme" 403 "$(status "$r")"

r=$(chamar POST /api/filmes "$TOKEN_ADMIN" "$NOVO_FILME")
conferir "ADMIN cadastra filme" 201 "$(status "$r")"
ID_FILME=$(numero "$(corpo "$r")" id)
exigir "o id do filme criado" "$ID_FILME"
detalhe "filme de teste = id $ID_FILME"

r=$(chamar POST /api/filmes "$TOKEN_ADMIN" "$NOVO_FILME")
conferir "titulo duplicado no mesmo ano e recusado" 409 "$(status "$r")"

r=$(chamar POST /api/filmes "$TOKEN_ADMIN" \
    '{"titulo":"Sem categoria","anoLancamento":2026,"duracaoMinutos":90,"categoriaIds":[]}')
conferir "filme sem categoria e recusado na validacao" 400 "$(status "$r")"

r=$(chamar POST /api/filmes "$TOKEN_ADMIN" \
    "{\"titulo\":\"Categoria inexistente\",\"anoLancamento\":2026,\"duracaoMinutos\":90,\"categoriaIds\":[999999]}")
conferir "categoria inexistente devolve 404" 404 "$(status "$r")"

r=$(chamar GET /api/usuarios "$TOKEN_USER" -)
conferir "USER nao lista contas" 403 "$(status "$r")"

r=$(chamar GET /api/usuarios "$TOKEN_ADMIN" -)
conferir "ADMIN lista contas" 200 "$(status "$r")"

titulo "5. Perfis e limite do plano"

r=$(chamar POST /api/perfis "$TOKEN_USER" '{"nome":"Principal"}')
conferir "nome de perfil repetido e recusado" 409 "$(status "$r")"

r=$(chamar GET /api/perfis/999999 "$TOKEN_USER" -)
conferir "perfil inexistente devolve 404" 404 "$(status "$r")"

titulo "6. Avaliacoes"

r=$(chamar POST "/api/perfis/$ID_PERFIL/avaliacoes" "$TOKEN_USER" \
    "{\"filmeId\":$ID_FILME,\"nota\":9,\"comentario\":\"nota invalida\"}")
conferir "nota fora de 1..5 e recusada" 400 "$(status "$r")"

r=$(chamar POST "/api/perfis/$ID_PERFIL/avaliacoes" "$TOKEN_USER" \
    "{\"filmeId\":$ID_FILME,\"nota\":4,\"comentario\":\"Bem feito.\"}")
conferir "avaliacao registrada" 201 "$(status "$r")"

r=$(chamar POST "/api/perfis/$ID_PERFIL/avaliacoes" "$TOKEN_USER" \
    "{\"filmeId\":$ID_FILME,\"nota\":5,\"comentario\":\"De novo.\"}")
conferir "avaliacao duplicada e recusada" 409 "$(status "$r")"

r=$(chamar GET "/api/filmes/$ID_FILME" "$TOKEN_USER" -)
conferir "detalhe do filme traz a nota media" 200 "$(status "$r")"
detalhe "$(printf '%s' "$(corpo "$r")" | grep -o '"notaMedia":[^,}]*') / $(printf '%s' "$(corpo "$r")" | grep -o '"totalAvaliacoes":[^,}]*')"

titulo "7. Posse dos recursos (ownership)"

# O ADMIN nao e dono dos perfis do assinante: perfil e dado pessoal.
r=$(chamar GET "/api/perfis/$ID_PERFIL" "$TOKEN_ADMIN" -)
conferir "ADMIN nao acessa perfil de outra conta" 403 "$(status "$r")"

r=$(chamar GET "/api/perfis/$ID_PERFIL/historico" "$TOKEN_ADMIN" -)
conferir "ADMIN nao acessa historico de outra conta" 403 "$(status "$r")"

r=$(chamar GET "/api/perfis/$ID_PERFIL/lista" "$TOKEN_ADMIN" -)
conferir "ADMIN nao acessa a lista de outra conta" 403 "$(status "$r")"

titulo "8. Minha Lista e historico"

r=$(chamar POST "/api/perfis/$ID_PERFIL/lista/$ID_FILME" "$TOKEN_USER" -)
conferir "adiciona filme a Minha Lista" 201 "$(status "$r")"

r=$(chamar POST "/api/perfis/$ID_PERFIL/lista/$ID_FILME" "$TOKEN_USER" -)
conferir "filme repetido na lista e recusado" 409 "$(status "$r")"

r=$(chamar DELETE "/api/perfis/$ID_PERFIL/lista/$ID_FILME" "$TOKEN_USER" -)
conferir "remove filme da Minha Lista" 204 "$(status "$r")"

r=$(chamar POST "/api/perfis/$ID_PERFIL/historico" "$TOKEN_USER" \
    "{\"filmeId\":$ID_FILME,\"minutosAssistidos\":30}")
conferir "registra progresso parcial" 200 "$(status "$r")"
detalhe "30 de 100 min -> $(printf '%s' "$(corpo "$r")" | grep -o '"concluido":[^,}]*')"

r=$(chamar POST "/api/perfis/$ID_PERFIL/historico" "$TOKEN_USER" \
    "{\"filmeId\":$ID_FILME,\"minutosAssistidos\":95}")
conferir "progresso acima de 90% conclui o filme" 200 "$(status "$r")"
detalhe "95 de 100 min -> $(printf '%s' "$(corpo "$r")" | grep -o '"concluido":[^,}]*')"

r=$(chamar POST "/api/perfis/$ID_PERFIL/historico" "$TOKEN_USER" \
    "{\"filmeId\":$ID_FILME,\"minutosAssistidos\":5000}")
conferir "progresso maior que a duracao e recusado" 409 "$(status "$r")"

r=$(chamar GET "/api/perfis/$ID_PERFIL/continuar-assistindo" "$TOKEN_USER" -)
conferir "consulta continuar assistindo" 200 "$(status "$r")"

titulo "9. Assinaturas"

r=$(chamar GET /api/assinaturas/ativa "$TOKEN_USER" -)
conferir "consulta a assinatura vigente" 200 "$(status "$r")"

r=$(chamar POST /api/assinaturas "$TOKEN_USER" "{\"planoId\":$ID_PLANO_PREMIUM}")
conferir "contratar o plano ja vigente e recusado" 409 "$(status "$r")"

r=$(chamar POST /api/assinaturas "$TOKEN_USER" "{\"planoId\":$ID_PLANO_PADRAO}")
conferir "troca de plano cancela a anterior" 200 "$(status "$r")"

r=$(chamar POST /api/assinaturas "$TOKEN_USER" "{\"planoId\":$ID_PLANO_PREMIUM}")
conferir "volta para o plano Premium" 200 "$(status "$r")"

titulo "10. Categorias"

r=$(chamar DELETE "/api/categorias/$ID_CATEGORIA" "$TOKEN_ADMIN" -)
conferir "categoria com filmes vinculados nao e excluida" 409 "$(status "$r")"

r=$(chamar POST /api/categorias "$TOKEN_ADMIN" \
    '{"nome":"Categoria Temporaria","descricao":"Criada pelo smoke test."}')
conferir "ADMIN cria categoria" 201 "$(status "$r")"
ID_CATEGORIA_NOVA=$(numero "$(corpo "$r")" id)

r=$(chamar DELETE "/api/categorias/$ID_CATEGORIA_NOVA" "$TOKEN_ADMIN" -)
conferir "categoria sem filmes e excluida" 204 "$(status "$r")"

titulo "11. Refresh token e logout"

r=$(chamar POST /api/auth/refresh - "{\"refreshToken\":\"$REFRESH_USER\"}")
conferir "refresh devolve novo par de tokens" 200 "$(status "$r")"
NOVO_REFRESH=$(texto "$(corpo "$r")" refreshToken)
NOVO_ACCESS=$(texto "$(corpo "$r")" token)

r=$(chamar POST /api/auth/refresh - "{\"refreshToken\":\"$REFRESH_USER\"}")
conferir "refresh token ja usado e recusado (rotacao)" 401 "$(status "$r")"

r=$(chamar POST /api/auth/logout "$NOVO_ACCESS" -)
conferir "logout encerra as sessoes" 204 "$(status "$r")"

r=$(chamar POST /api/auth/refresh - "{\"refreshToken\":\"$NOVO_REFRESH\"}")
conferir "refresh apos logout e recusado" 401 "$(status "$r")"

titulo "12. Upload de poster"

TEMP=$(mktemp -d)
# JPEG minimo: so a assinatura do formato, que e o que a validacao le.
printf '\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01' > "$TEMP/capa.jpg"
# Um texto com extensao e Content-Type de imagem: o caso que a validacao
# por assinatura existe para pegar.
printf 'isto aqui nao e uma imagem, e texto puro' > "$TEMP/falsa.jpg"

# No Git Bash do Windows o curl e um binario nativo e nao abre /tmp/...
# O MSYS converte caminhos automaticamente, mas desiste quando o argumento
# tem sufixo -- e ';type=' e exatamente isso. Convertemos na mao.
caminho_curl() {
  if command -v cygpath > /dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi
}

# enviar <caminho> <token> <arquivo> -> "<corpo>\n<status>"
# O ';type=image/jpeg' declara o tipo de proposito: e o que permite provar
# que a API nao acredita no que o cliente diz sobre o arquivo.
enviar() {
  curl -s -w '\n%{http_code}' -X POST "$BASE$1" \
       -H "Authorization: Bearer $2" -F "imagem=@$(caminho_curl "$3");type=image/jpeg"
}

r=$(enviar "/api/filmes/$ID_FILME/poster" "$TOKEN_USER" "$TEMP/capa.jpg")
conferir "USER nao envia poster" 403 "$(status "$r")"

r=$(enviar "/api/filmes/$ID_FILME/poster" "$TOKEN_ADMIN" "$TEMP/falsa.jpg")
conferir "texto disfarcado de JPEG e recusado" 400 "$(status "$r")"

r=$(enviar "/api/filmes/$ID_FILME/poster" "$TOKEN_ADMIN" "$TEMP/capa.jpg")
conferir "ADMIN envia poster" 200 "$(status "$r")"
POSTER_URL=$(texto "$(corpo "$r")" posterUrl)
exigir "a URL do poster enviado" "$POSTER_URL"
detalhe "poster gravado em $POSTER_URL"

case "$POSTER_URL" in
  /uploads/*) conferir "poster gravado na pasta de uploads" ok ok ;;
  *)          conferir "poster gravado na pasta de uploads" "/uploads/..." "$POSTER_URL" ;;
esac

# Sem token de proposito: a tag <img> do navegador nao manda Authorization.
CODIGO=$(curl -s -o /dev/null -w '%{http_code}' "$BASE$POSTER_URL")
conferir "imagem enviada abre sem autenticacao" 200 "$CODIGO"

r=$(chamar DELETE "/api/filmes/$ID_FILME/poster" "$TOKEN_ADMIN" -)
conferir "ADMIN remove o poster" 200 "$(status "$r")"

CODIGO=$(curl -s -o /dev/null -w '%{http_code}' "$BASE$POSTER_URL")
conferir "arquivo sai do disco junto com o poster" 404 "$CODIGO"

# O catalogo do seed usa imagens que viajam dentro do JAR.
r=$(chamar GET "/api/filmes?titulo=Matrix" "$TOKEN_USER" -)
POSTER_SEED=$(texto "$(corpo "$r")" posterUrl)
case "$POSTER_SEED" in
  /posters/*) conferir "filme do seed aponta para imagem local" ok ok ;;
  *)          conferir "filme do seed aponta para imagem local" "/posters/..." "$POSTER_SEED" ;;
esac

CODIGO=$(curl -s -o /dev/null -w '%{http_code}' "$BASE$POSTER_SEED")
conferir "poster do seed e servido pela aplicacao" 200 "$CODIGO"

rm -rf "$TEMP"

titulo "Limpeza"

r=$(chamar DELETE "/api/filmes/$ID_FILME" "$TOKEN_ADMIN" -)
conferir "ADMIN remove o filme de teste" 204 "$(status "$r")"

printf '\n=====================================\n'
verde "  Passou: $PASSOU"
if [ "$FALHOU" -gt 0 ]; then vermelho "  Falhou: $FALHOU"; else printf '  Falhou: 0\n'; fi
printf '=====================================\n'

[ "$FALHOU" -eq 0 ] && exit 0 || exit 1
