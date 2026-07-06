package com.example.transporte_interiorano.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.Carona
import com.example.transporte_interiorano.ui.theme.AzulPrincipal
import com.example.transporte_interiorano.ui.theme.VerdeBotao

@Composable
fun EditarEventoScreen(
    caronaInfo: Carona?,
    // ALTERADO: Agora passamos os dados salvos de volta como parâmetros
    aoSalvarAlteracao: (String, String, String, String, String, String, String) -> Unit,
    aoClicarVoltar: () -> Unit
) {
    // Estados que iniciam preenchidos com os dados atuais da carona selecionada
    var nomeEvento by remember { mutableStateOf(caronaInfo?.evento_nome ?: "") }
    var cidadeOrigem by remember { mutableStateOf(caronaInfo?.cidade_origem ?: "") }
    var enderecoOrigem by remember { mutableStateOf(caronaInfo?.endereco_origem ?: "") }
    var cidadeDestino by remember { mutableStateOf(caronaInfo?.cidade_destino ?: "") }
    var enderecoDestino by remember { mutableStateOf(caronaInfo?.endereco_destino ?: "") }
    var horario by remember { mutableStateOf(caronaInfo?.horario ?: "") }
    var vagas by remember { mutableStateOf(caronaInfo?.vagas ?: "") }

    var mensagemErro by remember { mutableStateOf("") }
    var estaCarregando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Cabeçalho ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Editar Informações",
                color = AzulPrincipal,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = aoClicarVoltar) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = AzulPrincipal)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Formulário de Campos ---
        OutlinedTextField(
            value = nomeEvento,
            onValueChange = { nomeEvento = it },
            label = { Text("Nome do Evento/Corrida") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = cidadeOrigem,
            onValueChange = { cidadeOrigem = it },
            label = { Text("📍 Cidade Origem") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = enderecoOrigem,
            onValueChange = { enderecoOrigem = it },
            label = { Text("Endereço de Origem Completo") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = cidadeDestino,
            onValueChange = { cidadeDestino = it },
            label = { Text("🏁 Cidade Destino") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = enderecoDestino,
            onValueChange = { enderecoDestino = it },
            label = { Text("Endereço de Destino Completo") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = horario,
            onValueChange = { horario = it },
            label = { Text("⏰ Horário de Partida") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = vagas,
            onValueChange = { vagas = it },
            label = { Text("👥 Número de Vagas Totais") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        if (mensagemErro.isNotEmpty()) {
            Text(
                text = mensagemErro,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // --- Botão de Salvar com Loading ---
        if (estaCarregando) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AzulPrincipal)
            }
        } else {

            Button(
                onClick = {
                    val ev = nomeEvento.trim()
                    val cidO = cidadeOrigem.trim()
                    val endO = enderecoOrigem.trim()
                    val cidD = cidadeDestino.trim()
                    val endD = enderecoDestino.trim()
                    val hor = horario.trim()
                    val vag = vagas.trim()

                    if (ev.isBlank() || cidO.isBlank() || endO.isBlank() ||
                        cidD.isBlank() || endD.isBlank() || hor.isBlank() || vag.isBlank()) {
                        mensagemErro = "Preencha todos os campos antes de salvar!"
                    } else {
                        mensagemErro = ""
                        estaCarregando = true // 🟢 Ativa o indicador visual na hora!
                        aoSalvarAlteracao(ev, cidO, endO, cidD, endD, hor, vag)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Salvar Alterações", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}