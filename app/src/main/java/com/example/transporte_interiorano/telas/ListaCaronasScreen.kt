package com.example.transporte_interiorano.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
                title = { Text("Transporte Interiorano", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = aoClicarVoltar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
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
    val meuPedido = pedidosDaCarona.find { it.passageiro == nomeLogado }

    val totalVagas = carona.vagas.toIntOrNull() ?: 0
    val qtdOcupadas = pedidosDaCarona.count {
        val status = it.status.lowercase()
        status.contains("aceito") || status.contains("pendente")
    }
    val vagasRestantes = totalVagas - qtdOcupadas

    // Organizando as informações completas para não cortar nada
    val partes = carona.origem.split(" - ", limit = 2)
    val eventoNome = if (partes.size > 1) partes[0] else "Evento"
    val origemReal = if (partes.size > 1) partes[1] else carona.origem
    val destinoReal = carona.destino

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            // 1. LINHA SUPERIOR: Foto e Nome
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Person, contentDescription = "Foto", tint = Color.Gray, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(carona.motorista, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("⭐ Novo Parceiro", fontSize = 12.sp, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. INFORMAÇÕES DA VIAGEM: Tudo 100% visível
            Text("🎯 Evento: $eventoNome", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AzulPrincipal)
            Spacer(modifier = Modifier.height(6.dp))
            Text("📍 Saída: $origemReal", fontSize = 13.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(2.dp))
            Text("🏁 Destino: $destinoReal", fontSize = 13.sp, color = Color.DarkGray)

            Spacer(modifier = Modifier.height(12.dp))

            // 3. HORÁRIO E VAGAS (Agora em coluna, um embaixo do outro)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "⏰ Partida: ${carona.horario}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(4.dp)) // Um pequeno espaço entre eles

                Text(
                    "👥 Vagas Livres: $vagasRestantes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if(vagasRestantes <= 0) VermelhoErro else VerdeBotao
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. ÁREA DOS BOTÕES
            val statusLimpo = meuPedido?.status?.trim()?.lowercase() ?: ""
            val ehExpirado = statusLimpo.contains("expirado")
            val ehRecusado = statusLimpo.contains("recusado")
            val ehAceito = statusLimpo.contains("aceito")
            val ehPendente = statusLimpo.contains("pendente")

// Essa variável decide se o botão aparece
            val mostrarBotaoSolicitar = (meuPedido == null || ehExpirado || ehRecusado)

// Agora usamos a lógica que criamos:
            if (meuPedido != null && !ehExpirado && !ehRecusado) {
                // ESTADO ATIVO: Aceito ou Pendente
                val corStatus = if (ehAceito) VerdeBotao else AmareloAviso
                val textoComEmoji = if (ehAceito) "Status: Aceito ✅" else "Status: Pendente ⏳"

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = corStatus.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp)) {
                        Text(textoComEmoji, color = corStatus, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }

                    if (ehPendente) {
                        OutlinedButton(
                            onClick = { BancoDeDados.cancelarPedidoPassageiro(meuPedido.idReal) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text("Cancelar", color = VermelhoErro, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (mostrarBotaoSolicitar) {
                // ESTADO FINALIZADO OU NOVO: Mostra status (se expirado/recusado) e botão Solicitar
                if (ehExpirado || ehRecusado) {
                    Text(
                        if (ehExpirado) "Status: Expirado ⏳" else "Status: Recusado ❌",
                        color = VermelhoErro, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (vagasRestantes > 0) {
                    Button(
                        onClick = { aoClicarEmSolicitar(carona) },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Solicitar Novamente", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Surface(modifier = Modifier.fillMaxWidth(), color = Color.LightGray.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)) {
                        Text("Esgotado", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 12.dp), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}