package com.example.transporte_interiorano.telas

import com.example.transporte_interiorano.BancoDeDados
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.Carona
import com.example.transporte_interiorano.ui.theme.*
//import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
//import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaCaronasScreen(
    nomeLogado: String,
    aoClicarEmSolicitar: (Carona) -> Unit,
    aoClicarVoltar: () -> Unit,
    aoClicarPerfil: () -> Unit,
    aoClicarHistorico: () -> Unit // 👈 Adicione esta linha
) {
    Scaffold(
        topBar = {
            // 💡 SUBSTITUA ESSE BLOCO DA TOPAPPBAR (Linhas 20 a 33):
            TopAppBar(
                title = {
                    Text("Transporte Interiorano", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = aoClicarVoltar) {
                        // 🟢 MUDANÇA: Substituído por seta de retorno para a tela de modalidades
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrincipal)
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F7)).padding(paddingValues).padding(16.dp)) {

            Text(
                "Eventos Disponíveis",
                color = Color.DarkGray,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )

            // 1. LÓGICA DE ORDENAÇÃO: Quem tem pedido sobe para o topo
            val caronasOrdenadas = remember(BancoDeDados.caronas, BancoDeDados.todosOsPedidos) {
                BancoDeDados.caronas.sortedByDescending { carona ->
                    val temPedido = BancoDeDados.todosOsPedidos.any {
                        it.caronaId == carona.id &&
                                it.passageiro.trim().equals(nomeLogado.trim(), ignoreCase = true)
                    }
                    if (temPedido) 1 else 0
                }
            }

            if (caronasOrdenadas.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Nenhum motorista disponível no momento.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    // 2. USANDO A LISTA ORDENADA
                    items(caronasOrdenadas) { carona ->
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

            OutlinedButton(
                onClick = aoClicarHistorico,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal)
            ) {
                Text("Ver Histórico de Viagens", fontWeight = FontWeight.Bold)
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

    //val vagasRestantes = carona.vagas.toIntOrNull() ?: 0
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

            // 2. INFORMAÇÕES (Tudo dentro da mesma coluna)
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🎯 Evento: ${carona.evento_nome}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal)

                Text("📍 Origem: ${carona.cidade_origem}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (carona.endereco_origem.isNotEmpty()) {
                    Text("   ${carona.endereco_origem}", fontSize = 12.sp, color = Color.Gray)
                }

                Text("🏁 Destino: ${carona.cidade_destino}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (carona.endereco_destino.isNotEmpty()) {
                    Text("   ${carona.endereco_destino}", fontSize = 12.sp, color = Color.Gray)
                }

                // Estes campos estavam sumindo porque podiam ter ficado fora da coluna ou o card estava pequeno
                Text("⏰ Partida: ${carona.horario}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("👥 Vagas Livres: $vagasRestantes", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (vagasRestantes <= 0) VermelhoErro else VerdeBotao)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. LÓGICA DE STATUS E BOTÕES
            val status = meuPedido?.status?.trim()?.lowercase() ?: ""

            if (meuPedido != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Status:", fontSize = 12.sp, color = Color.Gray)
                        Text(if (status.contains("aceito")) "Aceito ✅" else "Pendente ⏳",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (status.contains("aceito")) VerdeBotao else AmareloAviso)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // 🆕 O SEGREDO ESTÁ AQUI: O botão só aparece se for pendente!
                    if (status.contains("pendente")) {
                        OutlinedButton(
                            onClick = {
                                BancoDeDados.cancelarPedidoPassageiro(meuPedido.idReal)
                                BancoDeDados.buscarSolicitacoesDoServidor()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VermelhoErro)
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            } else if (vagasRestantes > 0) {
                Button(
                    onClick = { aoClicarEmSolicitar(carona) }, // Isto abre o DetalhesScreen
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
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