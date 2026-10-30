package com.rankfodinha.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/** Acesso aos dados da fodinha. As telas não sabem onde os dados ficam salvos. */
interface FodinhaRepository {
    suspend fun load(): FodinhaData
    suspend fun save(data: FodinhaData)
}

/** Formato dos dados gravados no aparelho. Mudar estas opções pode deixar dados já salvos ilegíveis. */
internal val FODINHA_JSON = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val Context.fodinhaDataStore: DataStore<Preferences> by preferencesDataStore(name = "fodinha")

/**
 * Repositório local: tudo fica em JSON no DataStore do aparelho.
 * Se ainda não existe o formato novo, os dados do formato antigo (chave v1) são convertidos ao carregar
 * e gravados no formato novo (chave v2) na primeira alteração. A chave antiga não é apagada.
 */
class LocalFodinhaRepository(
    private val dataStore: DataStore<Preferences>,
    private val clock: () -> Instant = { Instant.now() },
) : FodinhaRepository {

    override suspend fun load(): FodinhaData {
        val prefs = dataStore.data.first()
        prefs[DATA_KEY]?.let { return FODINHA_JSON.decodeFromString(FodinhaData.serializer(), it) }
        val legacy = prefs[LEGACY_DATA_KEY] ?: return FodinhaData()
        // Só converte na memória. Abrir o app não grava nada; o formato novo é salvo na primeira alteração.
        return migrateLegacyData(
            legacy = FODINHA_JSON.decodeFromString(LegacyFodinhaData.serializer(), legacy),
            migratedAt = clock().toString(),
        )
    }

    override suspend fun save(data: FodinhaData) {
        val raw = FODINHA_JSON.encodeToString(FodinhaData.serializer(), data)
        dataStore.edit { prefs -> prefs[DATA_KEY] = raw }
    }

    private companion object {
        /** Formato atual: sem palpites, com as vidas de cada jogador. */
        val DATA_KEY = stringPreferencesKey("fodinha_data_v2")

        /** Formato anterior, com palpites e vazas. Só é lido para migrar. */
        val LEGACY_DATA_KEY = stringPreferencesKey("fodinha_data_v1")
    }
}

fun createFodinhaRepository(context: Context): FodinhaRepository =
    LocalFodinhaRepository(context.applicationContext.fodinhaDataStore)
