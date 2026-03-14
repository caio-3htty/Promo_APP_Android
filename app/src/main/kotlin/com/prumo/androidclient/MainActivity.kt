package com.prumo.androidclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.prumo.core.model.ObraSummary
import com.prumo.data.repository.AppContainer
import com.prumo.feature.auth.LoginScreen
import com.prumo.feature.auth.LoginViewModel
import com.prumo.feature.estoque.EstoqueScreen
import com.prumo.feature.estoque.EstoqueViewModel
import com.prumo.feature.obras.ObrasScreen
import com.prumo.feature.obras.ObrasViewModel
import com.prumo.feature.pedidos.PedidosScreen
import com.prumo.feature.pedidos.PedidosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = (application as PromoAndroidApp).container

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PromoApp(container = container)
                }
            }
        }
    }
}

@Composable
private fun PromoApp(container: AppContainer) {
    val navController = rememberNavController()

    val mainViewModel: MainViewModel = viewModel(
        factory = simpleFactory { MainViewModel(container.authRepository) }
    )
    val state by mainViewModel.state.collectAsStateWithLifecycle()

    val loginViewModel: LoginViewModel = viewModel(
        factory = simpleFactory { LoginViewModel(container.authRepository) }
    )

    val obrasViewModel: ObrasViewModel = viewModel(
        factory = simpleFactory { ObrasViewModel(container.obrasRepository) }
    )

    val pedidosViewModel: PedidosViewModel = viewModel(
        factory = simpleFactory { PedidosViewModel(container.pedidosRepository) }
    )

    val estoqueViewModel: EstoqueViewModel = viewModel(
        factory = simpleFactory { EstoqueViewModel(container.estoqueRepository) }
    )

    LaunchedEffect(Unit) {
        mainViewModel.bootstrap()
    }

    LaunchedEffect(state.bootDone, state.session) {
        if (!state.bootDone) return@LaunchedEffect
        if (state.session == null) {
            navController.navigate("login") {
                popUpTo(0)
            }
        } else {
            navController.navigate("obras") {
                popUpTo(0)
            }
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            Text("Inicializando...", modifier = Modifier.padding(24.dp))
        }

        composable("login") {
            LoginScreen(viewModel = loginViewModel) {
                mainViewModel.bootstrap()
                navController.navigate("obras") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }

        composable("obras") {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Selecione a Obra", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = {
                        mainViewModel.logout()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }) {
                        Text("Sair")
                    }
                }

                ObrasScreen(viewModel = obrasViewModel) { obra ->
                    mainViewModel.selectObra(obra)
                    navController.navigate("home")
                }
            }
        }

        composable("home") {
            val selected = state.selectedObra
            if (selected == null) {
                Text("Nenhuma obra selecionada", modifier = Modifier.padding(16.dp))
            } else {
                HomeTabs(
                    selectedObra = selected,
                    pedidosViewModel = pedidosViewModel,
                    estoqueViewModel = estoqueViewModel,
                    onBackObras = { navController.navigate("obras") }
                )
            }
        }
    }
}

@Composable
private fun HomeTabs(
    selectedObra: ObraSummary,
    pedidosViewModel: PedidosViewModel,
    estoqueViewModel: EstoqueViewModel,
    onBackObras: () -> Unit
) {
    var tab by remember { mutableStateOf("pedidos") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(selectedObra.name, style = MaterialTheme.typography.titleLarge)
                Text("Obra ID: ${selectedObra.id}", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onBackObras) {
                Text("Trocar obra")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { tab = "pedidos" }) { Text("Pedidos") }
            Button(onClick = { tab = "estoque" }) { Text("Estoque") }
        }

        when (tab) {
            "pedidos" -> PedidosScreen(obraId = selectedObra.id, viewModel = pedidosViewModel)
            "estoque" -> EstoqueScreen(obraId = selectedObra.id, viewModel = estoqueViewModel)
        }
    }
}

private fun <T : ViewModel> simpleFactory(create: () -> T): ViewModelProvider.Factory {
    return object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
    }
}
