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
import com.example.transporte_interiorano.Pedido
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
            // 🟢 Puxa a lista Programada do Motorista
            BancoDeDados.buscarHistoricoMotoristaPorCpf(cpfUsuario) { listaProgramada ->
                // 🟢 Puxa em paralelo a lista Emergencial do Motorista
                BancoDeDados.buscarHistoricoEmergencialMotorista(cpfUsuario) { listaEmergencial ->
                    // Une e ordena as corridas decrescentemente com base na data do registro
                    val listaUnificada = (listaProgramada + listaEmergencial).sortedByDescending { it.horario }
                    historico = listaUnificada
                    carregando = false
                }
            }
        } else {
            // 🟢 Puxa a lista Programada do Passageiro
            BancoDeDados.buscarHistoricoPassageiroPorCpf(cpfUsuario) { listaProgramada ->
                // 🟢 Puxa em paralelo a lista Emergencial do Passageiro
                BancoDeDados.buscarHistoricoEmergencialPassageiro(cpfUsuario) { listaEmergencial ->
                    // Une e ordena as corridas decrescentemente com base na data do registro
                    val listaUnificada = (listaProgramada + listaEmergencial).sortedByDescending { it.horario }
                    historico = listaUnificada
                    carregando = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Viagens", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = aoClicarVoltar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AzulPrincipal
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (carregando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AzulPrincipal)
                }
            } else if (historico.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma viagem finalizada ainda.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(historico) { pedido ->
                        val ehEmergencial = pedido.evento_nome.contains("⚡")

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (ehEmergencial) Color(0xFFE8F5E9) else Color(0xFFF0F0F0) // 💡 Verde suave para diferenciar as emergenciais
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pedido.evento_nome,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (ehEmergencial) Color(0xFF2E7D32) else AzulPrincipal
                                    )

                                    if (isMotorista && pedido.passageiro.isNotEmpty()) {
                                        Text(
                                            text = "Passageiro: ${pedido.passageiro}",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    } else if (!isMotorista && pedido.motoristaNome.isNotEmpty()) {
                                        Text(
                                            text = "Motorista: ${pedido.motoristaNome}",
                                            fontSize = 11.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("De: ${pedido.cidade_origem} para ${pedido.cidade_destino}", fontSize = 14.sp)

                                // 🟢 LÓGICA DE CÁLCULO DO TEMPO TOTAL DA VIAGEM
                                val inicio = pedido.dataCriacao
                                val fim = pedido.dataFinalizacao
                                var tempoTotal = "--"

                                try {
                                    val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                    val dInicio = format.parse(inicio)
                                    val dFim = format.parse(fim)
                                    if (dInicio != null && dFim != null) {
                                        val diff = dFim.time - dInicio.time
                                        val minutos = (diff / (1000 * 60)) % 60
                                        val horas = (diff / (1000 * 60 * 60))
                                        tempoTotal = if (horas > 0) "${horas}h ${minutos}m" else "${minutos} min"
                                    }
                                } catch (e: Exception) { }

                                // 🟢 VISUAL DO RELATÓRIO DE TEMPO
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x0D000000), RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text("🕒 Início: $inicio", fontSize = 13.sp, color = Color.DarkGray)
                                    Text("🏁 Fim: $fim", fontSize = 13.sp, color = Color.DarkGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("⏱️ Tempo de Viagem: $tempoTotal", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (ehEmergencial) Color(0xFF2E7D32) else AzulPrincipal)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Status: Finalizada ✅", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}