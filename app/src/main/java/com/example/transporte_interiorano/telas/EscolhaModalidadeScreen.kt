package com.example.transporte_interiorano.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.ui.theme.*

@Composable
fun EscolhaModalidadeScreen(
    onModalidadeSelecionada: (String) -> Unit,
    onClicarFecharGeral: () -> Unit, // 🟢 1. O parâmetro adicionado exatamente na assinatura da função
    onClicarPerfil: () -> Unit, // 🟢 ADICIONADO: Parâmetro para disparar a abertura do Perfil
    onClicarHistorico: () -> Unit // 🟢 ADICIONADO: Parâmetro para abrir o histórico
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 🟢 2. Alinhamento superior esquerdo para renderizar o botão "X" igual ao app original
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onClicarFecharGeral) {
                Icon(
                    imageVector = Icons.Default.Close, // 💡 O ícone "X" para deslogar
                    contentDescription = "Sair do aplicativo",
                    tint = AzulPrincipal
                )
            }
        }

        // 🟢 3. O miolo com os cards centralizados verticalmente (agora ocupando o espaço restante)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Como deseja viajar hoje?",
                fontSize = 26.sp,
                color = AzulPrincipal,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 🟢 ADICIONADO: O botão "Ver Meu Perfil" posicionado exatamente abaixo do título antes dos cards
            Button(
                onClick = onClicarPerfil,
                colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Ver Meu Perfil", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 🟢 CORRIGIDO: Modificado ButtonDefaults para OutlinedButtonDefaults na propriedade colors
            OutlinedButton(
                onClick = onClicarHistorico,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(size = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal)
            ) {
                Text("Ver Histórico de Viagens", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CARD 1: VIAGEM PROGRAMADA
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clickable { onModalidadeSelecionada("Programada") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AzulPrincipal)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🗓️ Viagem Programada",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Agende caronas para o interior ou eventos com antecedência.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CARD 2: CORRIDA DE EMERGÊNCIA
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clickable { onModalidadeSelecionada("Emergencial") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)) // Verde para destacar a emergência
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚡ Corrida Emergencial",
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Precisa sair agora? Encontre motoristas nos bairros próximos.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}