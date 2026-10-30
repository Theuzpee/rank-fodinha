# Regras da fodinha no app

## Fontes consultadas

Pesquisadas para entender a fodinha tradicional. O app não implementa o jogo completo: usa só o controle de vidas (ver abaixo).

- Copag, "Fodinha: como jogar e quais as regras": https://blog.copag.com.br/regras/fodinha
  Doze vidas, perde a diferença entre palpite e vazas, cartas sobem e descem, 3 a 6 jogadores.
- HermanoLeite/fodinha (README): https://github.com/HermanoLeite/fodinha
  Cinco vidas, perde a diferença, 2 a 8 jogadores, cartas sobem até o limite e depois descem.
- Ludopedia, "Fodinha": https://ludopedia.com.br/jogo/fodinha
  Baralho espanhol, cinco vidas, perde a diferença. Para 4 jogadores, o máximo é 9 cartas.
- Marcador de Truco, "Fodinha - Estratégia Psicológica": https://www.marcadordetruco.com.br/fodinha-mastery
  Três a cinco vidas, perde 1 vida a cada erro.

As fontes divergem em vidas iniciais e penalidade. O app deixa a quantidade de vidas escolhível ao começar cada partida.
Os textos acima foram resumidos com as próprias palavras.

## Como o app funciona

- **Vidas**: cada jogador começa com a quantidade escolhida na nova partida (padrão 5, de 1 a 20). Todos começam igual.
- **Quem controla**: uma pessoa, com o celular na mão, toca em − quando um jogador perde uma vida, e em + para corrigir um toque errado.
- **Sem palpites**: não há palpites, vazas, baralhos nem penalidade automática. As vidas são sempre ajustadas à mão.
- **Eliminação**: quem chega a 0 vidas sai da partida. Enquanto a partida não termina, o + pode devolver as vidas.
- **Limite**: as vidas nunca ficam abaixo de zero.
- **Fim da partida**: quando sobra um único jogador com vida. Ele vence, e a partida entra no ranking.
- **Partida descartada**: pode ser cancelada enquanto está em andamento. Ela não entra no ranking.
- **Jogadores na partida**: todos os cadastrados entram por padrão. Dá para desmarcar quem não vai jogar.

## Ranking

- Entram só partidas encerradas.
- Ordem: mais vitórias primeiro. Empate em vitórias: maior aproveitamento (vitórias divididas por jogos). Depois, mais jogos, e por último o nome.
- Quem tem o mesmo número de vitórias e o mesmo aproveitamento divide a posição.
- **Limpar ranking** apaga todas as partidas encerradas. O ranking e a lista de partidas anteriores ficam vazios. A partida em andamento e os jogadores cadastrados não são afetados. Não dá para desfazer.

## Pontos em aberto

- Vidas iniciais padrão: as fontes citam 3, 5 e 12. O app usa 5, mas dá para escolher outro valor em cada partida.
