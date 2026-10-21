-- =====================================================================
-- TroiaFilmes - criacao do banco, para quem NAO usa o docker-compose
--
-- Com o Docker isto nao e necessario: o container ja nasce com o banco.
-- Usando um PostgreSQL instalado na maquina, rode UMA VEZ, conectado como
-- superusuario:
--
--   psql -U postgres -f docs/criar-banco.sql
--
-- Depois disso o Flyway cria todas as tabelas sozinho, no primeiro boot
-- da aplicacao. Usuario e senha da conexao ficam no application.properties
-- (postgres / lab008r2, os mesmos do arquivo passado em aula).
-- =====================================================================

-- template0 + UTF8: os scripts do Flyway tem acentuacao ("Ficção Científica").
CREATE DATABASE unesc
    WITH ENCODING = 'UTF8'
         TEMPLATE = template0;

COMMENT ON DATABASE unesc IS 'TroiaFilmes - Trabalho 1 de Desenvolvimento Backend com Spring Boot (UNESC)';
