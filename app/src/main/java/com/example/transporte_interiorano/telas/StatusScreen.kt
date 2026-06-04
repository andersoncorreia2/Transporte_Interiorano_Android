package com.example.transporte_interiorano.telas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.BancoDeDados
import com.example.transporte_interiorano.Carona
import com.example.transporte_interiorano.Pedido
import com.example.transporte_interiorano.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.filled.ArrowBack

@Composable
fun MinhasSolicitacoesScreen(
    isMotorista: Boolean, nomeMotoristaLogado: String, aoClicarPerfil: () -> Unit, aoClicarVoltar: () -> Unit, aoClicarNovoEvento: () -> Unit
) {
    val minhasCaronas = BancoDeDados.caronas.filter { it.motorista == nomeMotoristaLogado }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Minhas Solicitações", color = AzulPrincipal, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = aoClicarVoltar, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.height(36.dp)) {
                Text("🚪 Sair", color = VermelhoErro, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isMotorista) {
            Button(onClick = aoClicarNovoEvento, colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp)) {
                Text("➕ Criar Novo Evento/Corrida", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (minhasCaronas.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Nenhum evento criado por você no momento.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(minhasCaronas) { carona ->
                    CartaoEventoMotorista(carona)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = aoClicarPerfil, colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(8.dp)) {
            Text("Ver Meu Perfil")
        }
    }
}

@Composable
fun CartaoEventoMotorista(carona: Carona) {
    val pedidosDaCarona = BancoDeDados.todosOsPedidos.filter { it.caronaId == carona.id }
    val totalVagas = carona.vagas.toIntOrNull() ?: 0
    val qtdOcupadas = pedidosDaCarona.count {
        val status = it.status.lowercase()
        status.contains("aceito") || status.contains("pendente")
    }
    val vagasRestantes = totalVagas - qtdOcupadas

    //val partes = carona.origem.split(" - ", limit = 2)
    //val eventoNome = if (partes.size > 1) partes[0] else "Evento"
    //val origemReal = if (partes.size > 1) partes[1] else carona.origem

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Evento: ${carona.evento_nome}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal)
            Text(" ${carona.evento_nome}", fontSize = 12.sp, color = Color.Gray)

            // APAGUEI OS SPACERS DAQUI!
            Text("📍 Origem: ${carona.cidade_origem}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(" Endereço: ${carona.endereco_origem}", fontSize = 12.sp, color = Color.Gray)

            Text("🏁 Destino: ${carona.cidade_destino}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(" Endereço: ${carona.endereco_destino}", fontSize = 12.sp, color = Color.Gray)

            Text("⏰ Partida: ${carona.horario}", fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Text("👥 Vagas: $vagasRestantes/$totalVagas", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (vagasRestantes <= 0) VermelhoErro else VerdeBotao)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = Color.LightGray)

            // FILTRO NOVO: O motorista SÓ vê quem ainda não foi recusado/expirado
            val pedidosAtivos = pedidosDaCarona.filter {
                !it.status.lowercase().contains("recusado") && !it.status.lowercase().contains("expirado")
            }

            if (pedidosAtivos.isEmpty()) {
                Text("Nenhum pedido pendente ou aceito.", fontSize = 14.sp, color = Color.LightGray)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pedidosAtivos.forEach { pedido -> LinhaPassageiro(pedido, carona.motorista) }
                    //pedidosAtivos.forEach { pedido -> LinhaPassageiro(pedido) }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = { BancoDeDados.excluirCaronaDoServidor(carona.id) },
                modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = VermelhoErro)
            ) { Text("🗑️ Excluir Este Evento", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

@Composable
fun LinhaPassageiro(pedido: Pedido, caronaMotorista: String) {
    val statusLimpo = pedido.status.lowercase()
    var mostrarMotivo by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf("") }

    if (statusLimpo.contains("expirado")) return

    Surface(color = Color(0xFFF9F9F9), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                "🙋‍♂️ ${pedido.passageiro}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = AzulPrincipal
            )

            if (statusLimpo.contains("pendente")) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            BancoDeDados.responderPedidoMotorista(
                                pedido.idReal,
                                "Aceito"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Aceitar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { mostrarMotivo = true },
                        colors = ButtonDefaults.buttonColors(containerColor = VermelhoErro),
                        modifier = Modifier.weight(1f).height(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Recusar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                val textoStatus = if (statusLimpo.contains("aceito")) "Aceito ✅" else "Recusado ❌"
                val corStatus = if (statusLimpo.contains("aceito")) VerdeBotao else VermelhoErro

                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {

                    // 1. Texto do Status em cima
                    Text(
                        "Status: $textoStatus",
                        color = corStatus,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 2. Linha com os botões lado a lado
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Botão Retornar (o da setinha/desfazer)
                        IconButton(
                            onClick = {
                                BancoDeDados.responderPedidoMotorista(
                                    pedido.idReal,
                                    "Pendente"
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Yellow, RoundedCornerShape(8.dp))
                                .height(40.dp) // Defini uma altura para ficar alinhado com o outro
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Retornar")
                        }

                        // Botão Finalizar
                        Button(
                            onClick = {
                                BancoDeDados.finalizarCorridaNuvem(
                                    caronaMotorista,
                                    pedido.passageiro
                                )
                                android.util.Log.d("DEBUG_CLICK", "Finalização enviada!")
                            },
                            modifier = Modifier
                                .weight(2f)
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Blue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Finalizar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (mostrarMotivo) {
        AlertDialog(
            onDismissRequest = { mostrarMotivo = false },
            title = { Text("Motivo da recusa") },
            text = { TextField(value = motivo, onValueChange = { motivo = it }, label = { Text("Ex: Sem vagas") }) },
            confirmButton = {
                Button(onClick = {
                    val statusFinal = if(motivo.isNotEmpty()) "Recusado: $motivo" else "Recusado"
                    BancoDeDados.responderPedidoMotorista(pedido.idReal, statusFinal)
                    mostrarMotivo = false
                }) { Text("Confirmar") }
            }
        )
    }
}