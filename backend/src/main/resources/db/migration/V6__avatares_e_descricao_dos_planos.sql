-- =====================================================================
-- TroiaFilmes - V6: avatares locais e descricao dos planos
--
-- 1. Os perfis de demonstracao usavam avatares de um servico externo na
--    internet. Sem rede, a imagem nao carregava. Agora apontam para os
--    avatares que o proprio frontend serve (frontend/public/avatars).
--
-- 2. A descricao dos planos falava em "dispositivos", um conceito que o
--    sistema nao tem. O limite que o sistema aplica de verdade e o de
--    perfis (max_perfis), que ja aparece na tela separado da descricao.
--
-- As migrations ja publicadas (V1 a V5) nao podem ser editadas, entao a
-- mudanca entra como uma migration nova.
-- =====================================================================

UPDATE perfil SET avatar_url = '/avatars/azul.svg'    WHERE avatar_url LIKE '%dicebear%seed=principal';
UPDATE perfil SET avatar_url = '/avatars/amarelo.svg' WHERE avatar_url LIKE '%dicebear%seed=infantil';

UPDATE plano SET descricao = 'Para quem assiste sozinho.'                            WHERE nome = 'Básico';
UPDATE plano SET descricao = 'Para dividir a conta com mais uma pessoa.'             WHERE nome = 'Padrão';
UPDATE plano SET descricao = 'Para a família toda, com a melhor qualidade de imagem.' WHERE nome = 'Premium';
