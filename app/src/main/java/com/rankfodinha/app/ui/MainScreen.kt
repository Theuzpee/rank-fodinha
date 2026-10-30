package com.rankfodinha.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer
import com.rankfodinha.app.data.createFodinhaRepository
import com.rankfodinha.app.ui.components.LoadErrorScreen
import com.rankfodinha.app.ui.screens.JogadoresScreen
import com.rankfodinha.app.ui.screens.JogoScreen
import com.rankfodinha.app.ui.screens.NovaPartidaScreen
import com.rankfodinha.app.ui.screens.RankingScreen
import com.rankfodinha.app.viewmodel.FodinhaViewModel

private data class TabItem(val route: RootRoute, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(RootRoute.Jogo, "Jogo", Icons.Default.PlayArrow),
    TabItem(RootRoute.Ranking, "Ranking", Icons.Default.Star),
    TabItem(RootRoute.Jogadores, "Jogadores", Icons.Default.Person),
)

/** Altura da barra de abas, sem contar a área dos botões do sistema. A padrão do Material tem 80 dp. */
private val BOTTOM_BAR_HEIGHT = 64.dp

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val repository = remember { createFodinhaRepository(context) }
    val viewModel: FodinhaViewModel = viewModel { FodinhaViewModel(repository) }
    val loaded by viewModel.loaded.collectAsState()
    val loadError by viewModel.loadError.collectAsState()

    val startRoute: RootRoute = RootRoute.Jogo
    val topLevelRoutes: Set<NavKey> = tabs.map { it.route }.toSet()

    val navigationState = rememberNavigationState(
        startRoute = startRoute,
        topLevelRoutes = topLevelRoutes
    )

    val navigator = remember { Navigator(navigationState) }

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<RootRoute.Jogo> {
            JogoScreen(viewModel = viewModel, onNewGame = { navigator.navigate(RootRoute.NovaPartida) })
        }
        entry<RootRoute.Ranking> { RankingScreen(viewModel = viewModel) }
        entry<RootRoute.Jogadores> { JogadoresScreen(viewModel = viewModel) }
        entry<RootRoute.NovaPartida> {
            NovaPartidaScreen(viewModel = viewModel, onClose = { navigator.goBack() })
        }
    }

    val entries = navigationState.toEntries(entryProvider)
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull()
    // A tela de nova partida ocupa a tela toda, sem a barra de abas.
    val showBottomBar = currentRoute != RootRoute.NovaPartida

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    selected = navigationState.topLevelRoute,
                    onSelect = { navigator.navigate(it) },
                )
            }
        }
    ) { innerPadding ->
        if (loaded) {
            NavDisplay(
                entries = entries,
                onBack = { navigator.goBack() },
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                val message = loadError
                if (message != null) {
                    LoadErrorScreen(message = message, onRetry = viewModel::retryLoad)
                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/** Barra de abas em baixo, com o mesmo visual da barra padrão, mas mais baixa. */
@Composable
private fun AppBottomBar(selected: NavKey, onSelect: (NavKey) -> Unit) {
    Surface(
        color = NavigationBarDefaults.containerColor,
        tonalElevation = NavigationBarDefaults.Elevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(BOTTOM_BAR_HEIGHT)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { item ->
                BottomTab(
                    item = item,
                    selected = selected == item.route,
                    onClick = { onSelect(item.route) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Uma aba: ícone dentro de uma pílula quando está selecionada, e o nome embaixo. */
@Composable
private fun BottomTab(
    item: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) colors.secondaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Trata as ações de navegação (ir para frente e voltar), atualizando o estado de navegação.
 */
class Navigator(val state: NavigationState) {
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            // Rota de aba: só troca a aba atual.
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }

    fun goBack() {
        val currentStack = state.backStacks[state.topLevelRoute]
            ?: error("Stack for ${state.topLevelRoute} not found")

        // Se estiver na base da aba, volta para a aba inicial.
        if (currentStack.size == 1) {
            state.topLevelRoute = state.startRoute
        } else {
            currentStack.removeLastOrNull()
        }
    }
}

/**
 * Cria um estado de navegação que sobrevive a rotação de tela e a morte do processo.
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val topLevelRoute = rememberSerializable(
        startRoute, topLevelRoutes,
        serializer = MutableStateSerializer(NavKeySerializer())
    ) {
        mutableStateOf(startRoute)
    }

    val backStacks = topLevelRoutes.associateWith { key -> rememberNavBackStack(key) }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}

/**
 * Guarda o estado da navegação.
 *
 * @param startRoute rota inicial. Sair do app acontece por ela.
 * @param topLevelRoute aba atual.
 * @param backStacks pilha de voltar de cada aba.
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRoute
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) {
            listOf(startRoute)
        } else {
            listOf(startRoute, topLevelRoute)
        }
}

/**
 * Converte o estado de navegação em entradas para o NavDisplay.
 */
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>
): List<NavEntry<NavKey>> {
    val decoratedEntries = backStacks.mapValues { (_, stack) ->
        rememberDecoratedNavEntries(
            backStack = stack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator<NavKey>()),
            entryProvider = entryProvider
        )
    }

    return stacksInUse.flatMap { decoratedEntries[it] ?: emptyList() }
}
