package com.example.transporte_interiorano.telas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.ui.theme.*

// ALTERAÇÃO: Adicionamos mais 2 campos 'String' no 'aoPublicarEvento' para ele saber levar as cidades adiante.
@Composable
fun CriarEventoScreen(aoPublicarEvento: (String, String, String, String, String, String, String) -> Unit, aoClicarSair: () -> Unit) {
    var nomeEvento by remember { mutableStateOf("") }

    // INCLUSÃO: Criamos as gavetinhas na memória do celular para guardar o que o motorista digitar nas cidades.
    var cidadeOrigem by remember { mutableStateOf("") }
    var origem by remember { mutableStateOf("") }
    var cidadeDestino by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("") }
    var vagas by remember { mutableStateOf("") }

    // ⏰ MÁSCARA VISUAL DO HORÁRIO CORRIGIDA (Matemática exata do cursor)
    val mascaraHorario = VisualTransformation { text ->
        val num = text.text.take(4)
        var formatado = ""
        for (i in num.indices) {
            formatado += num[i]
            if (i == 1 && num.length > 2) formatado += ":"
        }
        val mapeamento = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset // 🛡️ CORREÇÃO: Mantém o cursor seguro nos 2 primeiros números
                if (offset <= 4) return offset + 1 // Empurra o cursor 1 casa pra frente depois que o ":" aparece
                return 5
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                return 4
            }
        }
        TransformedText(AnnotatedString(formatado), mapeamento)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp)) {

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

        // INCLUSÃO: Desenhando a caixinha da Cidade de Origem na tela.
        OutlinedTextField(
            value = cidadeOrigem,
            onValueChange = { cidadeOrigem = it },
            label = { Text("Cidade de Origem (Ex: Caruaru)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ALTERAÇÃO: Mudei o texto de "Origem" para "Endereço de Origem" para não confundir com a cidade.
        OutlinedTextField(
            value = origem,
            onValueChange = { origem = it },
            label = { Text("Endereço de Origem (Rua, Bairro)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // INCLUSÃO: Desenhando a caixinha da Cidade de Destino na tela.
        OutlinedTextField(
            value = cidadeDestino,
            onValueChange = { cidadeDestino = it },
            label = { Text("Cidade de Destino (Ex: Recife)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        // ALTERAÇÃO: Mudei o texto de "Destino" para "Endereço de Destino".
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
            // ⏰ CAMPO DE HORÁRIO ATUALIZADO COM A MÁSCARA SEGURA
            OutlinedTextField(
                value = horario,
                onValueChange = { novoValor ->
                    val apenasNumeros = novoValor.filter { it.isDigit() }
                    if (apenasNumeros.length <= 4) {
                        horario = apenasNumeros
                    }
                },
                label = { Text("Horário") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = mascaraHorario
            )

            // 🔢 CAMPO DE VAGAS
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

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                val horarioFinal = if (horario.length == 4) "${
                    horario.substring(
                        0,
                        2
                    )
                }:${horario.substring(2)}" else horario

                // ALTERAÇÃO: Agora o botão empacota as duas cidades novas junto com o resto das informações!
                aoPublicarEvento(
                    nomeEvento,
                    cidadeOrigem,
                    origem,
                    cidadeDestino,
                    destino,
                    horarioFinal,
                    vagas
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Publicar Corrida", fontSize = 16.sp)
        }
    }
}