package com.example.transporte_interiorano.telas

import com.example.transporte_interiorano.BancoDeDados//novo código
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.Carona
import com.example.transporte_interiorano.ui.theme.*
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun DetalhesScreen(caronaInfo: Carona?, aoConfirmarCarona: () -> Unit, aoClicarVoltar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {

        // 🔙 SETA DE VOLTAR CONSERTADA
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = aoClicarVoltar, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = AzulPrincipal)
            }
            Text(
                "Detalhes da Carona",
                color = AzulPrincipal,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (caronaInfo != null) {
            // 🆕 INÍCIO DO CÁLCULO DE VAGAS EM TEMPO REAL
            val pedidosDaCarona =
                BancoDeDados.todosOsPedidos.filter { it.caronaId == caronaInfo.id }
            val totalVagas = caronaInfo.vagas.toIntOrNull() ?: 0
            val qtdOcupadas = pedidosDaCarona.count {
                val status = it.status.lowercase()
                // Só conta vaga ocupada se for "aceito" ou "pendente" (ainda no prazo)
                status.contains("aceito") || status.contains("pendente")
            }
            val vagasRestantes = totalVagas - qtdOcupadas
            // 🆕 FIM DO CÁLCULO DE VAGAS EM TEMPO REAL

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AzulPrincipal)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Origem", fontSize = 12.sp, color = Color.Gray)
                    Text(caronaInfo.origem, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = VerdeBotao)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Destino", fontSize = 12.sp, color = Color.Gray)
                    Text(caronaInfo.destino, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Horário", fontSize = 12.sp, color = Color.Gray)
                    Text(caronaInfo.horario, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            //Texto novo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Vagas disponíveis", fontSize = 12.sp, color = Color.Gray)
                    // 🚨 TROQUE A VARIÁVEL AQUI PARA EXIBIR O CÁLCULO:
                    Text("$vagasRestantes vagas", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            //Texto antigo
            //Row(verticalAlignment = Alignment.CenterVertically) {
            //Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
            //Spacer(modifier = Modifier.width(16.dp))
            //Column {
            //Text("Vagas disponíveis", fontSize = 12.sp, color = Color.Gray)
            //Text("${caronaInfo.vagas} vagas", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            //}
            //}
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Motorista", fontSize = 12.sp, color = Color.Gray)
                    Text(caronaInfo.motorista, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$",
                    fontSize = 24.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Valor", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "Gratuito",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VerdeBotao
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 1. Defina o contexto logo antes do botão
        val context = LocalContext.current

        Button(
            onClick = {
                // Exibe o aviso para o passageiro
                Toast.makeText(
                    context,
                    "⚠️ Atenção: Você tem 15 minutos para efetuar o pagamento, ou a vaga será liberada!",
                    Toast.LENGTH_LONG
                ).show()

                // Executa a confirmação original
                aoConfirmarCarona()
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao)
        ) {
            Text("Confirmar Carona", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}