package com.rankfodinha.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rankfodinha.app.data.AVATAR_COUNT
import com.rankfodinha.app.data.FodinhaData
import com.rankfodinha.app.data.FodinhaRepository
import com.rankfodinha.app.data.Game
import com.rankfodinha.app.data.GameSettings
import com.rankfodinha.app.data.MAX_STARTING_LIVES
import com.rankfodinha.app.data.MIN_PLAYERS
import com.rankfodinha.app.data.MIN_STARTING_LIVES
import com.rankfodinha.app.data.Player
import com.rankfodinha.app.data.changeLives
import com.rankfodinha.app.data.createGame
import com.rankfodinha.app.data.isValidStartingLives
import java.time.Instant
import java.util.UUID
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Estado da fodinha. As telas observam [data] e chamam as ações deste ViewModel.
 * As ações que podem falhar retornam a mensagem de erro, ou null quando deram certo.
 * Nada é gravado enquanto os dados salvos não foram carregados, para não sobrescrevê-los.
 */
class FodinhaViewModel(private val repository: FodinhaRepository) : ViewModel() {

    private val _data = MutableStateFlow(FodinhaData())
    val data: StateFlow<FodinhaData> = _data.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Mensagem quando os dados salvos não puderam ser lidos. Nula enquanto está tudo bem. */
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    /** Uma gravação por vez, na ordem das alterações: a última versão é a que fica salva. */
    private val saveLock = Mutex()

    init {
        load()
    }

    /** Tenta ler os dados de novo, depois de uma falha. */
    fun retryLoad() {
        if (_loaded.value) return
        _loadError.value = null
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                _data.value = repository.load()
                _loadError.value = null
                _loaded.value = true
            } catch (error: Exception) {
                Log.e(TAG, "Não foi possível carregar os dados salvos.", error)
                _loadError.value = LOAD_ERROR_MESSAGE
            }
        }
    }

    /** Gera o id de um jogador novo. A foto dele é salva com esse id antes do cadastro, por isso o id vem antes. */
    fun newPlayerId(): String = newId()

    /**
     * Confere o nome de um jogador novo antes de tirar a foto. Devolve a mensagem de erro, ou null se o nome serve.
     * O nome precisa ter pelo menos 2 letras e não pode repetir outro, sem diferenciar maiúsculas.
     */
    fun checkNewPlayerName(rawName: String): String? {
        notLoadedError()?.let { return it }
        val name = normalizePlayerName(rawName)
        if (name.length < 2) {
            return "Digite um nome com pelo menos 2 letras."
        }
        if (_data.value.players.any { it.name.equals(name, ignoreCase = true) }) {
            return "Já existe um jogador com este nome."
        }
        return null
    }

    /**
     * Cadastra o jogador [playerId] com o nome dado. Todo jogador novo tem foto, então a foto com esse id
     * precisa estar salva antes desta chamada. O avatar sorteado da equipe fica só como reserva, se a foto sumir.
     * Devolve a mensagem de erro, ou null quando deu certo.
     */
    fun addPlayer(rawName: String, playerId: String): String? {
        checkNewPlayerName(rawName)?.let { return it }
        if (_data.value.players.any { it.id == playerId }) {
            return "Não foi possível cadastrar este jogador. Tente de novo."
        }
        val player = Player(
            id = playerId,
            name = normalizePlayerName(rawName),
            avatar = Random.nextInt(AVATAR_COUNT),
            photoSavedAt = now(),
        )
        mutate { data -> data.copy(players = data.players + player) }
        return null
    }

    fun removePlayer(playerId: String): String? {
        notLoadedError()?.let { return it }
        if (playerHasGames(playerId)) {
            return "Este jogador já participou de partidas e não pode ser removido."
        }
        mutate { data -> data.copy(players = data.players.filter { it.id != playerId }) }
        return null
    }

    /**
     * Marca que o jogador passou a usar a foto que tirou agora. A foto já deve estar salva no aparelho.
     * Se o jogador não existe mais, devolve erro, e quem chamou deve apagar essa foto.
     */
    fun setPlayerPhoto(playerId: String): String? {
        notLoadedError()?.let { return it }
        if (_data.value.players.none { it.id == playerId }) {
            return "Este jogador não existe mais."
        }
        val savedAt = now()
        mutate { data ->
            data.copy(players = data.players.map { if (it.id == playerId) it.copy(photoSavedAt = savedAt) else it })
        }
        return null
    }

    fun playerHasGames(playerId: String): Boolean =
        _data.value.games.any { playerId in it.playerIds }

    /** Começa uma partida com os jogadores da mesa, na ordem recebida, e as vidas iniciais escolhidas. */
    fun startGame(playerIds: List<String>, settings: GameSettings): String? {
        notLoadedError()?.let { return it }
        if (activeGame() != null) {
            return "Já existe uma partida em andamento. Encerre ou cancele a atual antes de começar outra."
        }
        if (playerIds.size < MIN_PLAYERS) {
            return "A mesa precisa de pelo menos $MIN_PLAYERS jogadores."
        }
        if (playerIds.distinct().size != playerIds.size) {
            return "Há jogadores repetidos na mesa."
        }
        val knownIds = _data.value.players.map { it.id }.toSet()
        if (playerIds.any { it !in knownIds }) {
            return "Algum jogador da mesa não existe mais."
        }
        if (!isValidStartingLives(settings.startingLives)) {
            return "As vidas iniciais precisam ser de $MIN_STARTING_LIVES a $MAX_STARTING_LIVES."
        }
        val game = createGame(id = newId(), startedAt = now(), playerIds = playerIds, settings = settings)
        mutate { data -> data.copy(games = data.games + game) }
        return null
    }

    /** Ajusta as vidas de um jogador da partida em andamento: -1 quando ele perde uma vida, +1 para corrigir. */
    fun adjustLives(playerId: String, delta: Int) {
        updateActiveGame { changeLives(it, playerId, delta, now()) }
    }

    /** Encerra a partida em andamento e registra quem ganhou. A partida passa a contar no ranking. */
    fun finishGame(winnerId: String): String? {
        notLoadedError()?.let { return it }
        val game = activeGame() ?: return "Não há partida em andamento."
        if (winnerId !in game.playerIds) {
            return "Escolha um jogador da partida."
        }
        mutate { data ->
            data.copy(
                games = data.games.map {
                    if (it.id == game.id) it.copy(finishedAt = now(), winnerIds = listOf(winnerId)) else it
                },
            )
        }
        return null
    }

    /** Descarta a partida em andamento. Ela não entra no histórico nem no ranking. */
    fun cancelGame() {
        mutate { data -> data.copy(games = data.games.filter { it.finishedAt != null }) }
    }

    /**
     * Apaga todas as partidas encerradas. O ranking e a lista de partidas anteriores ficam vazios.
     * A partida em andamento e os jogadores cadastrados não são afetados.
     */
    fun clearRanking() {
        mutate { data -> data.copy(games = data.games.filter { it.finishedAt == null }) }
    }

    private fun activeGame(): Game? = _data.value.games.firstOrNull { it.finishedAt == null }

    private fun updateActiveGame(change: (Game) -> Game) {
        mutate { data ->
            data.copy(games = data.games.map { if (it.finishedAt == null) change(it) else it })
        }
    }

    /** Atualiza o estado na hora, para a tela responder sem esperar o disco, e depois salva. */
    private fun mutate(change: (FodinhaData) -> FodinhaData) {
        if (!_loaded.value) return
        val next = change(_data.value)
        // Ação sem efeito não grava de novo.
        if (next == _data.value) return
        _data.value = next
        save(next)
    }

    private fun save(snapshot: FodinhaData) {
        viewModelScope.launch {
            saveLock.withLock { repository.save(snapshot) }
        }
    }

    private fun notLoadedError(): String? =
        if (_loaded.value) null else "Os dados ainda não foram carregados."

    private fun newId(): String = UUID.randomUUID().toString()

    /** Tira espaços das pontas e junta os espaços do meio em um só. */
    private fun normalizePlayerName(rawName: String): String = rawName.trim().replace(Regex("\\s+"), " ")

    private fun now(): String = Instant.now().toString()

    private companion object {
        const val TAG = "FodinhaViewModel"
        const val LOAD_ERROR_MESSAGE = "Não foi possível abrir os dados salvos no aparelho."
    }
}
