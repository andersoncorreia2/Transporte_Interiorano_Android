package com.example.transporte_interiorano.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    nome: String,
    email: String,
    veiculo: String,
    placa: String,
    aoClicarSair: () -> Unit,
    aoClicarVoltar: () -> Unit,
    aoClicarExcluirConta: () -> Unit,
    // 🆕 Truque: O valor padrão "= {}" evita que o MainActivity dê erro vermelho por falta de parâmetro!
    aoClicarEditar: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil", color = AzulPrincipal, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = aoClicarVoltar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = AzulPrincipal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Circular
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Person, contentDescription = "Avatar", tint = AzulPrincipal, modifier = Modifier.size(60.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(nome, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(email, fontSize = 14.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(32.dp))

            // Caixas de Informação
            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Membro desde", fontSize = 12.sp, color = Color.Gray)
                    Text("Maio de 2026", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Corridas realizadas", fontSize = 12.sp, color = Color.Gray)
                    Text("0 corridas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tipo da conta", fontSize = 12.sp, color = Color.Gray)
                    Text(if (veiculo.isNotEmpty()) "Motorista" else "Solicitante/Passageiro", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 🆕 NOVO BOTÃO: Editar Perfil
            Button(
                onClick = aoClicarEditar,
                colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Atualizar Dados", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = aoClicarSair,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = "Sair", tint = Color.Black, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da conta", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = aoClicarExcluirConta) {
                Text("Excluir minha conta permanentemente", color = VermelhoErro, fontWeight = FontWeight.Bold)
            }
        }
    }
}