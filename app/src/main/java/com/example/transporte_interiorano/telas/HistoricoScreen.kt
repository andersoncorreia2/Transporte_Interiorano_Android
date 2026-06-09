package com.example.transporte_interiorano.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.BancoDeDados
import com.example.transporte_interiorano.Pedido // Certifique-se de importar o Pedido
import com.example.transporte_interiorano.ui.theme.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(
    cpfUsuario: String,
    isMotorista: Boolean,
    aoClicarVoltar: () -> Unit
) {
    var historico by remember { mutableStateOf(listOf<Pedido>()) }
    var carregando by remember { mutableStateOf(true) }

    LaunchedEffect(cpfUsuario, isMotorista) {
        carregando = true
        if (isMotorista) {
            // Busca como MOTORISTA usando o CPF
            BancoDeDados.buscarHistoricoMotoristaPorCpf(cpfUsuario) { lista ->
                historico = lista
                carregando = false
            }
        } else {
            // Busca como PASSAGEIRO usando o CPF
            BancoDeDados.buscarHistoricoPassageiroPorCpf(cpfUsuario) { lista ->
                historico = lista
                carregando = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Viagens", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = aoClicarVoltar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulPrincipal,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (carregando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (historico.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma viagem finalizada ainda.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(historico) { pedido ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Evento: ${pedido.evento_nome}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AzulPrincipal)
                                Spacer(modifier = Modifier.height(4.dp))

                                Text("De: ${pedido.cidade_origem} para ${pedido.cidade_destino}")
                                Text("Data/Hora: ${pedido.horario}", fontSize = 12.sp)

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Status: Finalizada ✅", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}