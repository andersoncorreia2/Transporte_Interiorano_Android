package com.example.transporte_interiorano.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.BancoDeDados
import com.example.transporte_interiorano.Carona
import com.example.transporte_interiorano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCaronasScreen(nomeLogado: String, aoClicarEmSolicitar: (Carona) -> Unit, aoClicarVoltar: () -> Unit, aoClicarPerfil: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Transporte Interiorano", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = aoClicarVoltar) {
                        Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrincipal)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F7)).padding(paddingValues).padding(16.dp)) {

            Text("Motoristas Disponíveis Hoje", color = Color.DarkGray, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp, top = 8.dp))

            if (BancoDeDados.caronas.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum motorista disponível no momento.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(BancoDeDados.caronas) { carona ->
                        CartaoCaronaDisponivel(carona, nomeLogado, aoClicarEmSolicitar)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = aoClicarPerfil,
                colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Ver Meu Perfil", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CartaoCaronaDisponivel(carona: Carona, nomeLogado: String, aoClicarEmSolicitar: (Carona) -> Unit) {
    val pedidosDaCarona = BancoDeDados.todosOsPedidos.filter { it.caronaId == carona.id }
    val meuPedido = pedidosDaCarona
        .filter { it.passageiro.trim().equals(nomeLogado.trim(), ignoreCase = true) }
        .maxByOrNull { it.idReal }

    val totalVagas = carona.vagas.toIntOrNull() ?: 0
    val qtdOcupadas = pedidosDaCarona.count { it.status.lowercase().contains("aceito") || it.status.lowercase().contains("pendente") }
    val vagasRestantes = totalVagas - qtdOcupadas

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            // 1. FOTO E NOME
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(carona.motorista, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("⭐ Novo Parceiro", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 2. INFORMAÇÕES (Tudo coladinho com lineHeight)
            Text("🎯 Evento: ${carona.evento_nome}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal, lineHeight = 14.sp)
            Text("📍 Origem: ${carona.cidade_origem}", fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp)
            Text("   ${carona.endereco_origem}", fontSize = 12.sp, color = Color.Gray, lineHeight = 12.sp)
            Text("🏁 Destino: ${carona.cidade_destino}", fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 13.sp)
            Text("   ${carona.endereco_destino}", fontSize = 12.sp, color = Color.Gray, lineHeight = 12.sp)
            Text("⏰ Partida: ${carona.horario}", fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
            Text("👥 Vagas Livres: $vagasRestantes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (vagasRestantes <= 0) VermelhoErro else VerdeBotao, lineHeight = 14.sp)

            Spacer(modifier = Modifier.height(16.dp))

            // 3. LÓGICA DE STATUS E BOTÕES
            val status = meuPedido?.status?.trim()?.lowercase() ?: ""

            if (meuPedido != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val estaAceito = status.contains("aceito")
                    Surface(color = (if (estaAceito) VerdeBotao else AmareloAviso).copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text(if (estaAceito) "Status: Aceito ✅" else "Status: Pendente ⏳",
                            color = if (estaAceito) VerdeBotao else AmareloAviso,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp, 8.dp))
                    }
                    if (status.contains("pendente")) {
                        OutlinedButton(onClick = {
                            BancoDeDados.cancelarPedidoPassageiro(meuPedido.idReal)
                            BancoDeDados.buscarSolicitacoesDoServidor()
                        }, modifier = Modifier.height(40.dp)) {
                            Text("Cancelar", color = VermelhoErro, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (vagasRestantes > 0) {
                Button(onClick = {
                    aoClicarEmSolicitar(carona)
                    BancoDeDados.buscarSolicitacoesDoServidor()
                }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Solicitar Vaga", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(modifier = Modifier.fillMaxWidth(), color = Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)) {
                    Text("Esgotado", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center)
                }
            }
        }
    }
}