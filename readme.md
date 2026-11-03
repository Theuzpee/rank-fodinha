# Rank Fodinha

App Android nativo (Kotlin + Jetpack Compose) para acompanhar as vidas numa partida de fodinha entre colegas e montar um ranking.

## O que o app tem

- **Jogo**: partida em andamento. Cada jogador começa com a quantidade de vidas escolhida. Quando alguém perde uma vida, toque em − ao lado do nome. O + corrige um toque errado. Quem zera as vidas sai da partida, e o último com vida vence. Para encerrar antes, toque em **Finalizar partida** e escolha quem ganhou. Se escolher mais de um jogador, a partida termina em empate. Quando todos os jogadores com vida tiverem uma vida só, aparece o botão **Finalizar partida em empate**. Partidas antigas sem vencedor, ou sem a ordem das mortes, aparecem em **Partidas para completar**, logo abaixo de **Nova partida**. Toque em **Completar** para informar o que falta. Se nenhum jogador morreu antes dos outros, marque **Morreram juntos**, que não conta em Derrotas. Toque no avatar de um jogador para ver a foto em tela cheia.
- **Ranking**: vitórias, empates e derrotas, só com partidas encerradas que têm vencedor. Empate não conta como vitória. A seção **Derrotas** mostra quem mais vezes morreu primeiro em uma partida. Partidas feitas antes dessa versão não entram nela, porque não guardam a ordem das mortes. O botão **Limpar ranking** apaga as partidas encerradas (e com elas a lista de partidas anteriores), depois de pedir confirmação. Os jogadores continuam cadastrados.
- **Jogadores**: cadastro dos colegas. Todos os cadastrados entram na nova partida; dá para desmarcar quem não vai jogar.

As regras estão em [docs/regras-fodinha.md](docs/regras-fodinha.md).

## Como executar

1. Abra a pasta no Android Studio e aguarde o sync do Gradle.
2. Rode em um emulador ou em um aparelho Android (Android 8.0 ou superior).

Ou pelo terminal:

```bash
./gradlew assembleDebug
```

O APK gerado fica em `app/build/outputs/apk/debug/`.

Build de release, com minificação (R8) e redução de recursos ligadas:

```bash
./gradlew assembleRelease
```

O APK sai em `app/build/outputs/apk/release/app-release.apk`, assinado com a chave de debug deste computador (`~/.android/debug.keystore`). Ele serve para instalar direto no celular, não para publicar na loja. Para atualizar um app já instalado, a assinatura precisa ser a mesma. Se o arquivo `~/.android/debug.keystore` for apagado ou trocado, por exemplo ao usar outro computador, o app precisa ser desinstalado para instalar de novo, e os dados somem. Guarde uma cópia desse arquivo.

Testes:

```bash
./gradlew testDebugUnitTest          # regras, ranking, migração, ViewModel e persistência (sem aparelho)
./gradlew connectedDebugAndroidTest  # telas e layout (com um aparelho conectado)
```

Atenção: o `connectedDebugAndroidTest` desinstala o app ao final, e os dados do aparelho são apagados junto. Para rodar os testes de tela sem perder os dados, instale os APKs com `-r` e rode o `am instrument`. No fim, remova só o pacote de teste:

```bash
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r com.rankfodinha.app.test/androidx.test.runner.AndroidJUnitRunner
adb uninstall com.rankfodinha.app.test
```

Se tudo passou, a saída do `am instrument` termina com `OK (N tests)`.

## Dados

Os dados ficam salvos no próprio aparelho (DataStore). Cada aparelho tem o seu histórico.

Os dados salvos pela versão anterior, que tinha palpites e vazas, são convertidos na leitura. Mantêm-se a quantidade de vidas de cada jogador e as partidas encerradas. Palpites e vazas deixam de existir. O dado antigo não é apagado do aparelho. O formato novo só é gravado na primeira alteração feita no app, porque abrir o app não grava nada.

## Visual

O app usa só o tema claro, mesmo quando o aparelho está no modo escuro. A fonte é a Figtree, sob a licença SIL Open Font License (`licenses/FIGTREE-OFL.txt`). Os botões e destaques são verdes, e o botão de empate é âmbar. O vermelho aparece só em erros e em ações que apagam dados, como Remover e Limpar ranking. Ao cadastrar, o app abre a câmera para tirar a foto do jogador, e o cadastro só acontece depois da foto. Para trocar a foto depois, toque no avatar do jogador na aba Jogadores. As fotos ficam salvas só neste aparelho. Jogadores cadastrados antes dessa regra usam uma das 7 fotos da equipe até trocarem.

## Estrutura

```
app/src/main/java/com/rankfodinha/app/
├── data/        # modelos, regras das vidas, ranking, migração e repositório local
├── ui/          # telas, componentes, navegação e tema
└── viewmodel/   # FodinhaViewModel, com o estado do app
```
