package com.rankfodinha.app.viewmodel

import com.rankfodinha.app.data.FodinhaData
import com.rankfodinha.app.data.Player
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Erro ao carregar os dados salvos e nova tentativa. */
class LoadErrorTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun failingRepository(saved: FodinhaData = FodinhaData()): FakeFodinhaRepository {
        val repo = FakeFodinhaRepository(saved)
        repo.loadFailure = IOException("arquivo indisponível")
        return repo
    }

    private fun savedWithAna() = FodinhaData(players = listOf(Player("ana", "Ana")))

    @Test
    fun aFailedLoadShowsAMessageAndRetryLoadsTheData() {
        val repo = failingRepository(savedWithAna())
        val vm = FodinhaViewModel(repo)

        assertFalse(vm.loaded.value)
        assertNotNull(vm.loadError.value)

        repo.loadFailure = null
        vm.retryLoad()

        assertTrue(vm.loaded.value)
        assertNull(vm.loadError.value)
        assertEquals(listOf("Ana"), vm.data.value.players.map { it.name })
    }

    @Test
    fun aRetryThatFailsAgainKeepsTheMessageAndNothingIsSaved() {
        val repo = failingRepository(savedWithAna())
        val vm = FodinhaViewModel(repo)

        vm.retryLoad()

        assertFalse(vm.loaded.value)
        assertNotNull(vm.loadError.value)
        assertTrue("nada foi gravado por cima dos dados salvos", repo.saves.isEmpty())
    }

    @Test
    fun retryDoesNothingOnceTheDataIsLoaded() {
        val repo = FakeFodinhaRepository(savedWithAna())
        val vm = FodinhaViewModel(repo)

        vm.retryLoad()

        assertTrue(vm.loaded.value)
        assertNull(vm.loadError.value)
        assertEquals(listOf("Ana"), vm.data.value.players.map { it.name })
    }
}
