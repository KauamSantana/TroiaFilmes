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

## Entrega atual — Aula 8 (24/09): entidades e relacionamentos

Esta etapa entrega o **banco versionado** e o **mapeamento objeto-relacional**. Ainda não
há camada web: a aplicação sobe, aplica as migrations, confere se as entidades batem com
as tabelas e encerra.

O que existe hoje:

```
TroiaFilmes/
├── docker-compose.yml                  PostgreSQL 17 já configurado
└── backend/
    ├── pom.xml
    └── src/main/
        ├── java/br/edu/unesc/troiafilmes/
        │   ├── TroiaFilmesApplication.java
        │   └── entity/                 9 entidades + 3 enums + 1 de apoio
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
./mvnw spring-boot:run    # aplica as migrations e valida o mapeamento
```

> **Se você já tem PostgreSQL na máquina**, pare o serviço antes, senão os dois disputam a
> porta 5432 e a aplicação conecta no banco errado:
> `net stop postgresql-x64-17` (Windows, em terminal **como administrador**) ou
> `sudo systemctl stop postgresql` (Linux).

Ao final você deve ver no log:

```
Successfully applied 5 migrations to schema "public", now at version v5
```

e a aplicação encerrando sem erro. **Encerrar é o resultado esperado nesta etapa** — é o
que prova que as migrations rodaram e que o Hibernate validou as 11 tabelas contra as
entidades mapeadas.

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
| `V4__cria_refresh_token.sql` | `refresh_token`, para a autenticação JWT da próxima entrega |
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

## Credenciais

Todas de demonstração, criadas pelo `docker-compose.yml` e pela migration V5.

| Onde | Usuário | Senha |
|---|---|---|
| Banco de desenvolvimento | `postgres` | `lab008r2` |
| Conta ADMIN do seed | `admin@troiafilmes.com` | `Admin@123` |
| Conta USER do seed | `usuario@troiafilmes.com` | `User@123` |

As senhas das contas ficam no banco apenas como hash BCrypt. O login com elas passa a
funcionar quando a autenticação JWT entrar.

---

## Próximas entregas

Seguindo o cronograma do enunciado:

| Aula | Data | Conteúdo |
|---|---|---|
| 9 | 01/10 | Spring Security com JWT |
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
