-- =====================================================================
-- TroiaFilms - V5: massa de dados inicial para demonstracao
--
-- Senhas (hash BCrypt):
--   admin@troiafilms.com   -> Admin@123
--   usuario@troiafilms.com -> User@123
-- =====================================================================

-- ---------------------------------------------------------------- planos
INSERT INTO plano (nome, descricao, preco, max_perfis, qualidade_maxima, ativo) VALUES
    ('Básico',  'Um perfil, qualidade SD em um dispositivo por vez.',      19.90, 1, 'SD',  TRUE),
    ('Padrão',  'Até dois perfis, qualidade HD em dois dispositivos.',     34.90, 2, 'HD',  TRUE),
    ('Premium', 'Até cinco perfis, qualidade UHD em quatro dispositivos.', 54.90, 5, 'UHD', TRUE);

-- -------------------------------------------------------------- usuarios
INSERT INTO usuario (nome, email, senha, role, ativo) VALUES
    ('Administrador TroiaFilms', 'admin@troiafilms.com',   '$2a$10$YHoEbCQloUYPmEGvBZRxh.PC040/uf3eVzTehKLpUr45j5Q0coxt.', 'ADMIN', TRUE),
    ('Usuário Demonstração',     'usuario@troiafilms.com', '$2a$10$T7H3t6rZA1zP.X28EPigvu4dXqh9VpjzUrDYSRabvGt7zhgMZI/GO', 'USER',  TRUE);

-- assinatura Premium para o usuario de demonstracao (permite ate 5 perfis)
INSERT INTO assinatura (usuario_id, plano_id, data_inicio, status)
SELECT u.id, p.id, CURRENT_DATE, 'ATIVA'
  FROM usuario u, plano p
 WHERE u.email = 'usuario@troiafilms.com' AND p.nome = 'Premium';

-- --------------------------------------------------------------- perfis
INSERT INTO perfil (usuario_id, nome, avatar_url, infantil)
SELECT u.id, v.nome, v.avatar, v.infantil
  FROM usuario u
  JOIN (VALUES
        ('Principal', 'https://api.dicebear.com/9.x/bottts/svg?seed=principal', FALSE),
        ('Infantil',  'https://api.dicebear.com/9.x/bottts/svg?seed=infantil',  TRUE)
       ) AS v(nome, avatar, infantil) ON TRUE
 WHERE u.email = 'usuario@troiafilms.com';

-- ----------------------------------------------------------- categorias
INSERT INTO categoria (nome, descricao) VALUES
    ('Ação',              'Perseguições, confrontos e muita adrenalina.'),
    ('Aventura',          'Jornadas e descobertas em territórios desconhecidos.'),
    ('Comédia',           'Histórias leves feitas para arrancar risadas.'),
    ('Drama',             'Conflitos humanos e narrativas de forte carga emocional.'),
    ('Documentário',      'Narrativas construídas sobre fatos e personagens reais.'),
    ('Ficção Científica', 'Futuros possíveis, tecnologia e exploração espacial.'),
    ('Suspense',          'Tensão crescente e reviravoltas até o último minuto.'),
    ('Terror',            'Histórias construídas para assustar.'),
    ('Romance',           'Relações afetivas no centro da trama.'),
    ('Animação',          'Produções animadas para todas as idades.');

-- --------------------------------------------------------------- filmes
--
-- O poster_url e relativo de proposito: as imagens serao servidas pela
-- propria aplicacao quando a camada web entrar, entao o catalogo
-- aparece ilustrado mesmo sem internet -- inclusive na apresentacao.
INSERT INTO filme (titulo, sinopse, ano_lancamento, duracao_minutos, poster_url) VALUES
    ('Cidade de Deus',                            'Buscapé cresce numa favela carioca dominada pelo tráfico e encontra na fotografia a única saída possível para contar — e escapar de — a guerra que se arma ao seu redor.', 2002, 130, '/posters/cidade-de-deus.jpg'),
    ('Tropa de Elite 2: O Inimigo Agora É Outro', 'Promovido a subsecretário de inteligência, o coronel Nascimento descobre que tirar o tráfico do morro apenas abriu espaço para um inimigo mais bem vestido.', 2010, 115, '/posters/tropa-de-elite-2-o-inimigo-agora-e-outro.jpg'),
    ('Central do Brasil',                         'Uma ex-professora que escreve cartas para analfabetos na estação Central do Brasil acaba atravessando o país com um menino em busca do pai que ele nunca viu.', 1998, 110, '/posters/central-do-brasil.jpg'),
    ('O Auto da Compadecida',                     'No sertão paraibano, João Grilo e Chicó sobrevivem de trapaças — até que a conta chega e os dois precisam se explicar num julgamento nada comum.', 2000, 104, '/posters/o-auto-da-compadecida.jpg'),
    ('Bacurau',                                   'Um povoado do sertão desaparece do mapa logo após a morte de sua matriarca, e os moradores descobrem que estão sendo caçados.', 2019, 132, '/posters/bacurau.jpg'),
    ('Que Horas Ela Volta?',                      'Val é empregada doméstica há treze anos em São Paulo. A chegada da filha, que vem prestar vestibular, desarruma as regras não escritas da casa.', 2015, 112, '/posters/que-horas-ela-volta.jpg'),
    ('Senna',                                     'A trajetória de Ayrton Senna contada apenas com imagens de arquivo, do kart às três conquistas mundiais e ao acidente em Ímola.', 2010, 106, '/posters/senna.jpg'),
    ('Matrix',                                    'Um programador descobre que o mundo em que vive é uma simulação e precisa decidir se aceita enxergar a realidade por trás dela.', 1999, 136, '/posters/matrix.jpg'),
    ('Interestelar',                              'Com a Terra se tornando inabitável, um ex-piloto atravessa um buraco de minhoca em busca de um novo lar para a humanidade — e para a filha que deixou para trás.', 2014, 169, '/posters/interestelar.jpg'),
    ('A Origem',                                  'Um ladrão especializado em invadir sonhos recebe a tarefa inversa: em vez de roubar uma ideia, plantar uma.', 2010, 148, '/posters/a-origem.jpg'),
    ('Blade Runner 2049',                         'Trinta anos depois, um novo caçador de replicantes encontra um segredo enterrado capaz de desfazer o que restou da ordem social.', 2017, 164, '/posters/blade-runner-2049.jpg'),
    ('O Senhor dos Anéis: A Sociedade do Anel',   'Um hobbit herda um anel capaz de dominar a Terra Média e parte com oito companheiros para destruí-lo no único lugar possível.', 2001, 178, '/posters/o-senhor-dos-aneis-a-sociedade-do-anel.jpg'),
    ('Vingadores: Ultimato',                      'Depois da derrota para Thanos, os heróis remanescentes tentam uma última manobra para reverter o que foi perdido.', 2019, 181, '/posters/vingadores-ultimato.jpg'),
    ('Parasita',                                  'Uma família pobre se infiltra, um a um, no emprego de uma família rica — até que o porão da casa revela o que ninguém contava encontrar.', 2019, 132, '/posters/parasita.jpg'),
    ('Coringa',                                   'Um comediante fracassado de Gotham City vai sendo empurrado para a margem até que a cidade inteira sinta o resultado.', 2019, 122, '/posters/coringa.jpg'),
    ('Um Sonho de Liberdade',                     'Condenado por um crime que não cometeu, um bancário passa duas décadas em Shawshank cultivando a paciência como forma de resistência.', 1994, 142, '/posters/um-sonho-de-liberdade.jpg'),
    ('O Poderoso Chefão',                         'O filho caçula que recusava o negócio da família assume o comando da máfia Corleone depois de um atentado contra o pai.', 1972, 175, '/posters/o-poderoso-chefao.jpg'),
    ('Titanic',                                   'A bordo da viagem inaugural do maior navio já construído, dois passageiros de classes opostas se encontram poucos dias antes do naufrágio.', 1997, 194, '/posters/titanic.jpg'),
    ('La La Land: Cantando Estações',             'Uma atriz em início de carreira e um pianista de jazz se apaixonam em Los Angeles, enquanto perseguem sonhos que talvez não caibam no mesmo lugar.', 2016, 128, '/posters/la-la-land-cantando-estacoes.jpg'),
    ('O Iluminado',                               'Contratado como zelador de inverno de um hotel isolado, um escritor leva a família para um lugar que já tem seus próprios planos.', 1980, 146, '/posters/o-iluminado.jpg'),
    ('Corra!',                                    'Um jovem fotógrafo passa o fim de semana na casa dos pais da namorada e percebe, tarde demais, por que foi convidado.', 2017, 104, '/posters/corra.jpg'),
    ('Toy Story',                                 'O boneco favorito de um menino perde o posto para o brinquedo novo — e os dois precisam se entender para voltar para casa.', 1995,  81, '/posters/toy-story.jpg'),
    ('Divertida Mente',                           'As cinco emoções que comandam a cabeça de uma menina de onze anos entram em pane quando a família se muda de cidade.', 2015,  95, '/posters/divertida-mente.jpg'),
    ('O Rei Leão',                                'Depois da morte do pai, um jovem leão foge do reino carregando uma culpa que não é sua e precisa voltar para ocupar seu lugar.', 1994,  88, '/posters/o-rei-leao.jpg');

-- ------------------------------------------ vinculo N:N filme <-> categoria
INSERT INTO filme_categoria (filme_id, categoria_id)
SELECT f.id, c.id
  FROM (VALUES
        ('Cidade de Deus', 'Drama')                             , ('Cidade de Deus', 'Ação'),
        ('Tropa de Elite 2: O Inimigo Agora É Outro', 'Ação')   , ('Tropa de Elite 2: O Inimigo Agora É Outro', 'Drama'),
        ('Central do Brasil', 'Drama')                          , ('O Auto da Compadecida', 'Comédia'),
        ('O Auto da Compadecida', 'Aventura')                   , ('Bacurau', 'Suspense'),
        ('Bacurau', 'Ação')                                     , ('Que Horas Ela Volta?', 'Drama'),
        ('Senna', 'Documentário')                               , ('Matrix', 'Ficção Científica'),
        ('Matrix', 'Ação')                                      , ('Interestelar', 'Ficção Científica'),
        ('Interestelar', 'Drama')                               , ('A Origem', 'Ficção Científica'),
        ('A Origem', 'Suspense')                                , ('Blade Runner 2049', 'Ficção Científica'),
        ('Blade Runner 2049', 'Suspense')                       , ('O Senhor dos Anéis: A Sociedade do Anel', 'Aventura'),
        ('O Senhor dos Anéis: A Sociedade do Anel', 'Ação')     , ('Vingadores: Ultimato', 'Ação'),
        ('Vingadores: Ultimato', 'Aventura')                    , ('Parasita', 'Suspense'),
        ('Parasita', 'Drama')                                   , ('Coringa', 'Drama'),
        ('Coringa', 'Suspense')                                 , ('Um Sonho de Liberdade', 'Drama'),
        ('O Poderoso Chefão', 'Drama')                          , ('O Poderoso Chefão', 'Suspense'),
        ('Titanic', 'Romance')                                  , ('Titanic', 'Drama'),
        ('La La Land: Cantando Estações', 'Romance')            , ('La La Land: Cantando Estações', 'Comédia'),
        ('O Iluminado', 'Terror')                               , ('O Iluminado', 'Suspense'),
        ('Corra!', 'Terror')                                    , ('Corra!', 'Suspense'),
        ('Toy Story', 'Animação')                               , ('Toy Story', 'Aventura'),
        ('Divertida Mente', 'Animação')                         , ('Divertida Mente', 'Comédia'),
        ('O Rei Leão', 'Animação')                              , ('O Rei Leão', 'Aventura')
       ) AS v(titulo, categoria)
  JOIN filme     f ON f.titulo = v.titulo
  JOIN categoria c ON c.nome   = v.categoria;

-- ----------------------------------------- avaliacoes do perfil Principal
INSERT INTO avaliacao (perfil_id, filme_id, nota, comentario)
SELECT p.id, f.id, v.nota, v.comentario
  FROM (VALUES
        ('Cidade de Deus',        5, 'Fotografia e montagem impecáveis. Envelheceu muito bem.'),
        ('Interestelar',          5, 'A parte da relatividade me pegou de jeito.'),
        ('Parasita',              4, 'Começa como comédia e termina como outra coisa. Genial.'),
        ('O Auto da Compadecida', 5, 'Assisto de novo toda vez que passa na TV.'),
        ('Blade Runner 2049',     3, 'Lindo de ver, mas longo demais para o meu gosto.')
       ) AS v(titulo, nota, comentario)
  JOIN filme   f ON f.titulo = v.titulo
  JOIN perfil  p ON p.nome   = 'Principal'
  JOIN usuario u ON u.id = p.usuario_id AND u.email = 'usuario@troiafilms.com';

-- ------------------------------------------ Minha Lista do perfil Principal
INSERT INTO item_lista (perfil_id, filme_id)
SELECT p.id, f.id
  FROM filme f
  JOIN perfil  p ON p.nome = 'Principal'
  JOIN usuario u ON u.id = p.usuario_id AND u.email = 'usuario@troiafilms.com'
 WHERE f.titulo IN ('Bacurau', 'Senna', 'O Poderoso Chefão', 'Corra!');

-- ----------------------------------- historico ("continuar assistindo")
INSERT INTO historico_visualizacao (perfil_id, filme_id, minutos_assistidos, concluido)
SELECT p.id, f.id, v.minutos, v.concluido
  FROM (VALUES
        ('Cidade de Deus',                          130, TRUE),
        ('Interestelar',                            169, TRUE),
        ('Matrix',                                   40, FALSE),
        ('Titanic',                                  60, FALSE),
        ('O Senhor dos Anéis: A Sociedade do Anel',  95, FALSE)
       ) AS v(titulo, minutos, concluido)
  JOIN filme   f ON f.titulo = v.titulo
  JOIN perfil  p ON p.nome   = 'Principal'
  JOIN usuario u ON u.id = p.usuario_id AND u.email = 'usuario@troiafilms.com';
