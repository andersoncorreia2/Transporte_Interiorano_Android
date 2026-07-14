package com.example.transporte_interiorano.telas

import com.example.transporte_interiorano.BancoDeDados
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun DetalhesScreen(
    caronaInfo: Carona?,
    nomePassageiroLogado: String, // ADICIONADO: Necessário para buscar o status do passageiro
    corridasIniciais: Int,
    passageirosIniciais: Int,
    aoConfirmarCarona: () -> Unit,
    aoClicarVoltar: () -> Unit
) {
    var corridas by remember { mutableStateOf(corridasIniciais) }
    var passageiros by remember { mutableStateOf(passageirosIniciais) }

    LaunchedEffect(caronaInfo?.motorista_cpf) {
        if (caronaInfo != null && caronaInfo.motorista_cpf.isNotEmpty()) {
            BancoDeDados.buscarMétricasPorCpf(caronaInfo.motorista_cpf) { c, p ->
                corridas = c
                passageiros = p
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = aoClicarVoltar, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = AzulPrincipal)
            }
            Text(
                "Detalhes da Corrida",
                color = AzulPrincipal,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (caronaInfo != null) {
            val pedidosDaCarona =
                BancoDeDados.todosOsPedidos.filter { it.caronaId == caronaInfo.id }

            // 🆕 LÓGICA DE STATUS: Busca o pedido específico deste passageiro
            //val meuPedido = pedidosDaCarona.find {
                //it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase()
            //}

            val totalVagas = caronaInfo.vagas.toIntOrNull() ?: 0
            val qtdOcupadas = pedidosDaCarona.count {
                val status = it.status.lowercase()
                // Só conta vaga ocupada se for "aceito" ou "pendente" (ainda no prazo)
                status.contains("aceito") || status.contains("pendente")
            }
            //val meuPedido = pedidosDaCarona.find {
                //it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase() &&
                        //(it.status.lowercase() == "pendente" || it.status.lowercase() == "aceito")
            //}
            val vagasRestantes = totalVagas - qtdOcupadas

            // Campos Origem/Destino/Horário
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = AzulPrincipal)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Origem", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${caronaInfo.cidade_origem} - ${caronaInfo.endereco_origem}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = VerdeBotao)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Destino", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "${caronaInfo.cidade_destino} - ${caronaInfo.endereco_destino}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
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

                    // Aqui os valores são exibidos exatamente como solicitado
                    Text("Corridas realizadas: $corridas", fontSize = 12.sp, color = Color.DarkGray)
                    Text(
                        "Passageiros conduzidos: $passageiros",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seção Valor
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
                Text("Gratuito", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VerdeBotao)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 1. Defina o pedido do passageiro
        val pedidosDaCarona = BancoDeDados.todosOsPedidos.filter { it.caronaId == caronaInfo?.id }
        val meuPedido = pedidosDaCarona
            .filter { it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase() }
            .sortedByDescending { it.idReal }
            .firstOrNull { it.status.lowercase() != "finalizado" }

        // 🕵️ RASTREADOR DE HISTÓRICO: Verifica se o último registro dela para essa viagem foi marcado como Expirado
        val foiExpiradoAnteriormente = pedidosDaCarona.any {
            it.passageiro.trim().lowercase() == nomePassageiroLogado.trim().lowercase() &&
                    it.status.equals("Expirado", ignoreCase = true)
        }

        // 2. Lógica para exibir o status (se existir pedido ativo)
        if (meuPedido != null) {
            Text(
                "Status atual: ${meuPedido.status}",
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally),
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
        }

        // 3. Botão ÚNICO de Confirmar Vaga com Texto Inteligente
        val context = LocalContext.current
        Button(
            onClick = {
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
            // 🟢 SEPARAÇÃO EXCLUSIVA DE TEXTO: Altera a mensagem dinâmica na tela se ela perdeu o prazo antes
            Text(
                text = if (foiExpiradoAnteriormente) "Confirmar Vaga Novamente 🔄" else "Confirmar Vaga",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } // Fecha a Column
} // Fecha a função DetalhesScreen