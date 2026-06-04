package com.example.transporte_interiorano.telas

import android.app.DatePickerDialog // 🟢 INCLUÍDO: Ferramenta do Calendário
import android.app.TimePickerDialog // 🟢 INCLUÍDO: Ferramenta do Relógio
import java.util.Calendar           // 🟢 INCLUÍDO: Sabe a data de hoje
import androidx.compose.ui.platform.LocalContext // 🟢 INCLUÍDO: Permite abrir janelas na tela
import androidx.compose.foundation.clickable     // 🟢 INCLUÍDO: Permite clicar em coisas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.ui.theme.*

@Composable
fun CriarEventoScreen(
    aoPublicarEvento: (String, String, String, String, String, String, String, String) -> Unit,
    aoClicarSair: () -> Unit,
    cpfLogado: String
) {
    // 🟢 INCLUÍDO: Ferramentas essenciais para o Calendário funcionar na tela
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var nomeEvento by remember { mutableStateOf("") }
    var cidadeOrigem by remember { mutableStateOf("") }
    var origem by remember { mutableStateOf("") }
    var cidadeDestino by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("") } // Agora vai guardar "DD/MM/AAAA às HH:MM"
    var vagas by remember { mutableStateOf("") }

    // 🔴 EXCLUÍDO: A "mascaraHorario" foi totalmente removida daqui porque não precisamos mais digitar!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = aoClicarSair, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Sair", tint = AzulPrincipal)
            }
            Text(
                "Novo Evento/Corrida",
                color = AzulPrincipal,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Para onde vamos hoje?", fontSize = 16.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nomeEvento,
            onValueChange = { nomeEvento = it },
            label = { Text("Nome do Evento (Ex: Culto de Domingo)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cidadeOrigem,
            onValueChange = { cidadeOrigem = it },
            label = { Text("Cidade de Origem (Ex: Caruaru)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = origem,
            onValueChange = { origem = it },
            label = { Text("Endereço de Origem (Rua, Bairro)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cidadeDestino,
            onValueChange = { cidadeDestino = it },
            label = { Text("Cidade de Destino (Ex: Recife)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = destino,
            onValueChange = { destino = it },
            label = { Text("Endereço de Destino (Rua, Bairro)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 🔄 SUBSTITUIÇÃO: A Mágica do Calendário começa aqui
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = horario,
                    onValueChange = { },
                    readOnly = true, // Bloqueia o teclado, não deixa digitar letras
                    label = { Text("Data e Hora") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Caixa invisível que fica por cima e recebe o clique do dedo
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable {
                            // 1. Abre a tela de escolher o DIA
                            DatePickerDialog(
                                context,
                                { _, ano, mes, dia ->
                                    // 2. Quando o motorista escolhe o dia, abre a tela de HORA
                                    TimePickerDialog(
                                        context,
                                        { _, hora, minuto ->
                                            // 3. Junta tudo num texto bonitinho!
                                            val diaFormatado = dia.toString().padStart(2, '0')
                                            val mesFormatado = (mes + 1).toString().padStart(2, '0')
                                            val horaFormatada = hora.toString().padStart(2, '0')
                                            val minFormatado = minuto.toString().padStart(2, '0')

                                            horario = "$diaFormatado/$mesFormatado/$ano às $horaFormatada:$minFormatado"
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true // Usa o formato de 24 horas (ex: 19:00)
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                )
            }

            OutlinedTextField(
                value = vagas,
                onValueChange = { novoValor ->
                    val apenasNumeros = novoValor.filter { it.isDigit() }
                    vagas = apenasNumeros
                },
                label = { Text("Vagas") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // 🔴 EXCLUÍDO: O cortador de horário saiu daqui, enviamos o texto pronto
                aoPublicarEvento(
                    nomeEvento,
                    cidadeOrigem,
                    origem,
                    cidadeDestino,
                    destino,
                    horario, // 🟢 INCLUÍDO: Enviando o texto completão para o banco de dados!
                    vagas,
                    cpfLogado
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Publicar Corrida", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}