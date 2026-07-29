package com.example.transporte_interiorano.telas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
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
import com.example.transporte_interiorano.BancoDeDados // 🟢 IMPORTADO
import com.example.transporte_interiorano.ui.theme.*

@Composable
fun CriarEventoScreen(
    aoPublicarEvento: (String, String, String, String, String, String, String, String, String, String) -> Unit,
    aoClicarSair: () -> Unit,
    cpfLogado: String,
    nomeLogado: String
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var nomeEvento by remember { mutableStateOf("") }
    var cidadeOrigem by remember { mutableStateOf("") }
    var origem by remember { mutableStateOf("") }
    var cidadeDestino by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("") }
    var vagas by remember { mutableStateOf("") }
    var valorCorrida by remember { mutableStateOf("") }

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
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = horario,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Data e Hora") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, ano, mes, dia ->
                                    TimePickerDialog(
                                        context,
                                        { _, hora, minuto ->
                                            val diaFormatado = dia.toString().padStart(2, '0')
                                            val mesFormatado = (mes + 1).toString().padStart(2, '0')
                                            val horaFormatada = hora.toString().padStart(2, '0')
                                            val minFormatado = minuto.toString().padStart(2, '0')

                                            horario = "$diaFormatado/$mesFormatado/$ano às $horaFormatada:$minFormatado"
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
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
                    vagas = novoValor.filter { it.isDigit() }
                },
                label = { Text("Vagas") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // 🟢 CAMPO: Valor Total da Corrida definido pelo motorista
            OutlinedTextField(
                value = valorCorrida,
                onValueChange = { valorCorrida = it },
                label = { Text("Valor Total (R$)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                aoPublicarEvento(
                    nomeEvento,
                    cidadeOrigem,
                    origem,
                    cidadeDestino,
                    destino,
                    horario,
                    vagas,
                    valorCorrida,
                    nomeLogado,  // 🟢 9º Parâmetro (Nome)
                    cpfLogado    // 🟢 10º Parâmetro (CPF)
                )
                // 🟢 ADICIONADO: Força o gatilho de sincronização imediata no ato da publicação
                BancoDeDados.buscarCaronasDoServidor()
                BancoDeDados.buscarSolicitacoesDoServidor()
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