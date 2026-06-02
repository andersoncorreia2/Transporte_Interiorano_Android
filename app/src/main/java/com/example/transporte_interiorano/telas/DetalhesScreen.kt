package com.example.transporte_interiorano.telas

import com.example.transporte_interiorano.BancoDeDados//novo código
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // 🆕 IMPORTAÇÃO NECESSÁRIA PARA REMEMBER E LAUNCHEDEFFECT
//import androidx.compose.runtime.Composable
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

    // 🆕 VARIÁVEIS DE ESTADO PARA ATUALIZAR EM TEMPO REAL
    var corridasMotorista by remember { mutableStateOf(caronaInfo?.corridas_realizadas ?: 0) }
    var passageirosMotorista by remember { mutableStateOf(caronaInfo?.passageiros_conduzidos ?: 0) }

    // 🆕 BUSCA AS MÉTRICAS DO MOTORISTA ASSIM QUE A TELA ABRE
    LaunchedEffect(caronaInfo) {
        if (caronaInfo != null) {
            // Nota: Se você mudou para buscar por e-mail no BancoDeDados.kt,
            // você deve passar o e-mail aqui. Se a função ainda aceita nome, mantenha assim:
            BancoDeDados.buscarMétricasDoUsuario(caronaInfo.motorista) { c, p ->
                corridasMotorista = c
                passageirosMotorista = p
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = aoClicarVoltar, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = AzulPrincipal)
            }
            Text("Detalhes das Corridas", color = AzulPrincipal, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (caronaInfo != null) {
            // 🆕 SEU CÁLCULO DE VAGAS ORIGINAL
            val pedidosDaCarona = BancoDeDados.todosOsPedidos.filter { it.caronaId == caronaInfo.id }
            val totalVagas = caronaInfo.vagas.toIntOrNull() ?: 0
            val qtdOcupadas = pedidosDaCarona.count {
                val status = it.status.lowercase()
                status.contains("aceito") || status.contains("pendente")
            }
            val vagasRestantes = totalVagas - qtdOcupadas

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AzulPrincipal)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Origem", fontSize = 12.sp, color = Color.Gray)
                    Text("${caronaInfo.cidade_origem} - ${caronaInfo.endereco_origem}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = VerdeBotao)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Destino", fontSize = 12.sp, color = Color.Gray)
                    Text("${caronaInfo.cidade_destino} - ${caronaInfo.endereco_destino}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Vagas disponíveis", fontSize = 12.sp, color = Color.Gray)
                    Text("$vagasRestantes vagas", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Motorista", fontSize = 12.sp, color = Color.Gray)
                    Text(caronaInfo.motorista, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    // 🆕 AGORA USA AS VARIÁVEIS DE ESTADO QUE ATUALIZAM SOZINHAS
                    Text("Corridas realizadas: $corridasMotorista", fontSize = 12.sp, color = Color.DarkGray)
                    Text("Passageiros conduzidos: $passageirosMotorista", fontSize = 12.sp, color = Color.DarkGray)
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
            Text("Confirmar Carrida", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}