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
import com.example.transporte_interiorano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricoScreen(aoClicarVoltar: () -> Unit) {
    // Filtra apenas as caronas com status 'Finalizada' que estão no seu BancoDeDados
    val historico = BancoDeDados.caronas.filter { it.status == "Finalizada" }

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
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (historico.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nenhuma viagem finalizada ainda.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(historico) { carona ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Evento: ${carona.evento_nome}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("De: ${carona.cidade_origem} para ${carona.cidade_destino}")
                                Text("Data/Hora: ${carona.horario}", fontSize = 12.sp)
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