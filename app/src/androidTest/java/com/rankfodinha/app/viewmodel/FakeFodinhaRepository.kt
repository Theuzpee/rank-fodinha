package com.rankfodinha.app.viewmodel

import com.rankfodinha.app.data.FodinhaData
import com.rankfodinha.app.data.FodinhaRepository

/** Repositório em memória para os testes de interface. */
class FakeFodinhaRepository(initial: FodinhaData = FodinhaData()) : FodinhaRepository {
    private var stored: FodinhaData = initial

    override suspend fun load(): FodinhaData = stored

    override suspend fun save(data: FodinhaData) {
        stored = data
    }
}
