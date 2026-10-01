# TroiaFilmes

API REST de um serviço de streaming de filmes.
Trabalho 1 — Desenvolvimento Backend com Spring Boot · UNESC · Prof. Matheus Leandro Ferreira.

| Integrante | Domínio sob responsabilidade |
|---|---|
| Kauam Sant'ana | Usuário, Plano, Assinatura |
| Estefani Ricardo de Souza | Filme, Categoria |
| Gabrielle Coral | Perfil, Item de Lista |
| Ingridy Morona Rosa | Avaliação, Histórico de Visualização |

---

## Entrega atual — Aula 9 (01/10): Spring Security com JWT

Esta etapa liga a **autenticação** à API: cadastro de conta, login com e-mail e senha, e
um token JWT que precisa acompanhar toda requisição às rotas protegidas. Ela se apoia na
entrega da Aula 8 (banco versionado com Flyway e pacote `entity`).

O que existe hoje:

```
TroiaFilmes/
├── docker-compose.yml                  PostgreSQL 17 já configurado
└── backend/
    ├── pom.xml
    └── src/main/
        ├── java/br/edu/unesc/troiafilmes/
        │   ├── TroiaFilmesApplication.java
        │   ├── config/                 SecurityConfig, SecurityFilter, TokenConfig, AuthConfig
        │   ├── controller/             AuthController
        │   ├── dto/request/            LoginRequest, RegistroUsuarioRequest
        │   ├── dto/response/           LoginResponse, RegistroUsuarioResponse, UsuarioLogadoResponse
        │   ├── entity/                 9 entidades + 3 enums + 1 de apoio
        │   └── repository/             UsuarioRepository
        └── resources/
            ├── application.properties
            └── db/migration/           V1 a V5 (Flyway)
```

---

## Como rodar

Você precisa de **Docker** e **JDK 21 ou superior**. Não precisa ter PostgreSQL instalado.

```bash
docker compose up -d      # sobe o banco
cd backend
./mvnw spring-boot:run    # aplica as migrations e sobe a API na porta 8080
```

> **Se você já tem PostgreSQL na máquina**, pare o serviço antes, senão os dois disputam a
> porta 5432 e a aplicação conecta no banco errado:
> `net stop postgresql-x64-17` (Windows, em terminal **como administrador**) ou
> `sudo systemctl stop postgresql` (Linux).

Ao final você deve ver no log:

```
Successfully applied 5 migrations to schema "public", now at version v5
Tomcat started on port 8080 (http)
Started TroiaFilmesApplication
```

A API fica no ar em `http://localhost:8080`. Os exemplos de uso estão na seção
[Autenticação](#autenticação-jwt).

Para conferir o banco por dentro:

```bash
docker exec -it troiafilmes-db psql -U postgres -d unesc -c "\dt"
```

---

## Migrations (Flyway)

O Flyway é a **única fonte da verdade do schema**. O Hibernate está em
`ddl-auto=validate`, ou seja, ele não cria nem altera nada — apenas confere. Se alguém
mexer numa entidade e esquecer a migration correspondente, a aplicação não sobe. É uma
barreira proposital: o banco nunca é alterado por acidente.

| Script | O que cria |
|---|---|
| `V1__cria_tabelas_core.sql` | `usuario`, `perfil`, `filme`, `categoria`, `avaliacao` e a associativa `filme_categoria` |
| `V2__cria_tabelas_assinatura.sql` | `plano` e `assinatura` |
| `V3__cria_tabelas_engajamento.sql` | `item_lista` (Minha Lista) e `historico_visualizacao` |
| `V4__cria_refresh_token.sql` | `refresh_token`, reservada para a renovação de sessão (ainda não usada) |
| `V5__seed_dados_iniciais.sql` | Massa inicial: 3 planos, 10 categorias, 24 filmes, 2 contas e dados de demonstração |

### Regras que já moram no banco

Nem toda regra fica no código. Estas o banco garante sozinho, mesmo sob requisições
simultâneas:

- `UNIQUE (perfil_id, filme_id)` em `avaliacao` — um perfil avalia cada filme uma só vez.
- `UNIQUE (perfil_id, filme_id)` em `item_lista` — sem item repetido na Minha Lista.
- `CHECK (nota BETWEEN 1 AND 5)` em `avaliacao`.
- Índice único **parcial** `uk_assinatura_ativa_por_usuario ON assinatura (usuario_id) WHERE status = 'ATIVA'`
  — um usuário pode ter várias assinaturas no histórico, mas só uma ativa por vez.
- `ON DELETE CASCADE` nas dependências de `filme` e `perfil`.

> **Se uma migration falhar no meio**, o PostgreSQL reverte as tabelas, mas **não** as
> sequences — elas são não-transacionais por natureza, então os ids deixam de começar em 1.
> Para voltar ao estado limpo: `docker compose down -v && docker compose up -d`.

---

## Pacote `entity`

Nove entidades de domínio, três enums e a entidade de apoio `RefreshToken`.

| Entidade | Papel |
|---|---|
| `Usuario` | Conta de acesso: e-mail único, senha com hash, papel (`ADMIN`/`USER`) e ativação lógica |
| `Perfil` | Perfis dentro de uma conta, no modelo "quem está assistindo" |
| `Filme` | Título do catálogo |
| `Categoria` | Gênero |
| `Avaliacao` | Nota de 1 a 5 e comentário, de um perfil para um filme |
| `Plano` | Plano de assinatura: preço, limite de perfis e qualidade máxima |
| `Assinatura` | Vínculo entre conta e plano, com situação e vigência |
| `ItemLista` | Minha Lista — associativa entre perfil e filme, com a data em que foi salvo |
| `HistoricoVisualizacao` | Progresso de reprodução, base do "Continuar assistindo" |
| `RefreshToken` | Token de renovação de sessão, persistido para poder ser revogado |

### Relacionamentos

**1:N** — `Usuario`→`Perfil`, `Usuario`→`Assinatura`, `Plano`→`Assinatura`,
`Perfil`→`Avaliacao`, `Filme`→`Avaliacao`, `Perfil`→`ItemLista`, `Filme`→`ItemLista`,
`Perfil`→`HistoricoVisualizacao`, `Filme`→`HistoricoVisualizacao`, `Usuario`→`RefreshToken`.

**N:N** — resolvidos de duas formas diferentes, de propósito:

- **`Filme` ↔ `Categoria`** com `@ManyToMany` e tabela de junção simples. O vínculo não
  tem nada a dizer além de existir.
- **`Perfil` ↔ `Filme`** (Minha Lista) através da entidade associativa **`ItemLista`**.
  Aqui o vínculo carrega informação própria (`adicionadoEm`), e relacionamento com atributo
  não cabe num `@ManyToMany`.

### Decisões de mapeamento

- **`FetchType.LAZY` em todo `@ManyToOne`.** O padrão do JPA é `EAGER`, que traz o grafo
  inteiro em cada consulta.
- **`@EqualsAndHashCode(of = "id")`.** Comparar todos os campos em entidade JPA dispara
  carregamento de coleções lazy dentro do `hashCode`.
- **`@Builder.Default` nas coleções**, para o builder do Lombok não substituir a lista
  inicializada por `null`.
- **Enums com `@Enumerated(EnumType.STRING)`.** Guardar o ordinal quebra os dados assim
  que alguém reordena o enum.

---

## Autenticação (JWT)

Com o Spring Security, **toda rota nasce bloqueada**. Só duas ficam abertas, porque
acontecem antes de existir um token: o cadastro e o login.

| Método | Rota | Acesso | O que faz |
|---|---|---|---|
| `POST` | `/api/auth/registrar` | público | Cria uma conta com papel `USER` |
| `POST` | `/api/auth/login` | público | Confere e-mail e senha e devolve o token |
| `GET` | `/api/auth/usuario-logado` | com token | Mostra de quem é o token enviado |

### Como funciona

1. O login confere a senha contra o hash BCrypt do banco, através do `AuthenticationManager`.
2. Se estiver certa, o `TokenConfig` gera um JWT assinado com HMAC256, válido por 24 horas.
   O token carrega o e-mail (`sub`), o id do usuário e o papel.
3. Nas requisições seguintes, o cliente envia `Authorization: Bearer <token>`.
4. O `SecurityFilter` lê o header, confere assinatura, emissor e validade, e coloca o
   usuário no contexto de segurança. A API é **stateless**: não há sessão no servidor.

A entidade `Usuario` implementa `UserDetails`: o login é o e-mail, e o papel vira a
autoridade `ROLE_ADMIN` ou `ROLE_USER`. Conta desativada (`ativo = false`) não faz login,
e um token emitido antes da desativação deixa de valer na hora.

### Exemplos

**Cadastro**

```bash
curl -X POST http://localhost:8080/api/auth/registrar \
  -H "Content-Type: application/json" \
  -d '{"nome": "Estefani Souza", "email": "estefani@exemplo.com", "senha": "Senha@123"}'
```

```json
HTTP 201
{ "nome": "Estefani Souza", "email": "estefani@exemplo.com" }
```

**Login**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "usuario@troiafilmes.com", "senha": "User@123"}'
```

```json
HTTP 200
{ "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
```

**Rota protegida**

```bash
curl http://localhost:8080/api/auth/usuario-logado \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

```json
HTTP 200
{ "id": 2, "nome": "Usuário Demonstração", "email": "usuario@troiafilmes.com", "role": "USER" }
```

### Respostas de erro

| Situação | Código | Mensagem |
|---|---|---|
| Campo inválido ou faltando | `400` | Lista cada campo e o motivo, ex.: "A senha deve ter entre 8 e 72 caracteres." |
| E-mail ou senha errados, ou conta desativada | `401` | "E-mail ou senha inválidos." |
| Rota protegida sem token, ou token adulterado, vencido ou de outra chave | `401` | "Token ausente, inválido ou expirado." |
| Cadastro com e-mail já existente | `409` | "Já existe uma conta com este e-mail." |

O e-mail não diferencia maiúsculas: `Fulano@Email.com` e `fulano@email.com` são a mesma
conta. A mensagem de login é a mesma para e-mail inexistente e senha errada, de propósito,
para a API não revelar quais e-mails têm conta.

---

## Credenciais

Todas de demonstração, criadas pelo `docker-compose.yml` e pela migration V5.

| Onde | Usuário | Senha |
|---|---|---|
| Banco de desenvolvimento | `postgres` | `lab008r2` |
| Conta ADMIN do seed | `admin@troiafilmes.com` | `Admin@123` |
| Conta USER do seed | `usuario@troiafilmes.com` | `User@123` |

As senhas das contas ficam no banco apenas como hash BCrypt. Use as duas contas em
`POST /api/auth/login` para obter um token.

---

## Próximas entregas

Seguindo o cronograma do enunciado:

| Aula | Data | Conteúdo |
|---|---|---|
| 10 | 08/10 | Camadas Controller, Service e Repository, DTOs e tratamento de exceções |
| 11 | 15/10 | Swagger e ajustes |
| 12 | 22/10 | Projeto final, frontend e apresentação |

---

## Configuração

O `application.properties` segue o modelo passado em aula. As diferenças estão comentadas
no próprio arquivo; em resumo:

- `spring.flyway.enable` foi corrigido para **`spring.flyway.enabled`**. Escrito errado, o
  Spring ignora a linha sem avisar — funcionava por acidente, já que o Flyway vem ligado
  por padrão.
- Acrescentado `spring.flyway.encoding=UTF-8`, porque os scripts têm acentuação e sem isso
  o Flyway usa a codificação do sistema operacional.
- A chave que assina os tokens fica em `troiafilmes.jwt.secret`, e não no código. O valor
  do arquivo serve só para desenvolvimento: fora daqui, defina a variável de ambiente
  `JWT_SECRET`. **Quem conhece a chave consegue fabricar tokens válidos.**
