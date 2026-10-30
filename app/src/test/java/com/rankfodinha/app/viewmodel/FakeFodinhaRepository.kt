package com.rankfodinha.app.viewmodel

import com.rankfodinha.app.data.FodinhaData
import com.rankfodinha.app.data.FodinhaRepository

/** Repositório em memória para testar o ViewModel. Guarda cada gravação para conferir o que foi salvo. */
class FakeFodinhaRepository(initial: FodinhaData = FodinhaData()) : FodinhaRepository {

    /** Se não for nulo, a leitura falha com esta exceção. */
    var loadFailure: Exception? = null

    var stored: FodinhaData = initial
        private set

    val saves = mutableListOf<FodinhaData>()

    override suspend fun load(): FodinhaData {
        loadFailure?.let { throw it }
        return stored
    }

    override suspend fun save(data: FodinhaData) {
        stored = data
        saves += data
    }
}
