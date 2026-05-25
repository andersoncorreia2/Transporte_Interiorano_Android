package com.example.transporte_interiorano.telas

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.BancoDeDados
import com.example.transporte_interiorano.Usuario
import com.example.transporte_interiorano.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPerfilScreen(
    usuarioAtual: Usuario,
    aoSalvar: (Usuario) -> Unit,
    aoCancelar: () -> Unit
) {
    var nome by remember { mutableStateOf(usuarioAtual.nome) }
    var telefone by remember { mutableStateOf(usuarioAtual.telefone) }
    var veiculo by remember { mutableStateOf(usuarioAtual.veiculo) }
    var placa by remember { mutableStateOf(usuarioAtual.placa) }
    var vagas by remember { mutableStateOf( usuarioAtual.vagas) }

    var cep by remember { mutableStateOf(usuarioAtual.cep) }
    var rua by remember { mutableStateOf(usuarioAtual.rua) }
    var numero by remember { mutableStateOf(usuarioAtual.numero) }
    var complemento by remember { mutableStateOf(usuarioAtual.complemento) }
    var bairro by remember { mutableStateOf(usuarioAtual.bairro) }
    var cidade by remember { mutableStateOf(usuarioAtual.cidade) }
    var estado by remember { mutableStateOf(usuarioAtual.estado) }

    var ofertarCarona by remember { mutableStateOf(usuarioAtual.veiculo.isNotEmpty()) }
    var salvando by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atualizar Dados", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = aoCancelar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Cancelar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AzulPrincipal)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F7F7))
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("DADOS PESSOAIS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome completo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = telefone,
                onValueChange = { telefone = it.filter { c -> c.isDigit() }.take(11) },
                label = { Text("Telefone / WhatsApp") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), // <-- A CORREÇÃO ESTÁ AQUI
                visualTransformation = TelefoneVisualTransformation()
            )

            Text("ENDEREÇO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = cep, onValueChange = { cep = it.take(8) }, label = { Text("CEP") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = estado, onValueChange = { estado = it.take(2).uppercase() }, label = { Text("UF") }, modifier = Modifier.weight(0.5f))
            }
            OutlinedTextField(value = cidade, onValueChange = { cidade = it }, label = { Text("Cidade") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = rua, onValueChange = { rua = it }, label = { Text("Rua/Avenida") }, modifier = Modifier.weight(2f))
                OutlinedTextField(value = numero, onValueChange = { numero = it }, label = { Text("Nº") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = complemento, onValueChange = { complemento = it }, label = { Text("Complemento") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = bairro, onValueChange = { bairro = it }, label = { Text("Bairro") }, modifier = Modifier.weight(1f))
            }

            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ofertar Corridas", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(checked = ofertarCarona, onCheckedChange = { ofertarCarona = it })
                    }
                    if (ofertarCarona) {
                        OutlinedTextField(value = veiculo, onValueChange = { veiculo = it }, label = { Text("Veículo") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = placa, onValueChange = { placa = it }, label = { Text("Placa") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField( // 🆕 CAMPO DE VAGAS
                            value = vagas,
                            onValueChange = { vagas = it },
                            label = { Text("Vagas Totais") },
                            modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            Button(
                onClick = {
                    salvando = true
                    val usuarioAtualizado = usuarioAtual.copy(
                        nome = nome, telefone = telefone, veiculo = if (ofertarCarona) veiculo else "",
                        placa = if (ofertarCarona) placa else "", vagas = if (ofertarCarona) vagas else "0", rua = rua, numero = numero,
                        complemento = complemento, bairro = bairro, cidade = cidade, estado = estado, cep = cep
                    )
                    BancoDeDados.atualizarUsuarioNuvem(usuarioAtualizado) { sucesso ->
                        salvando = false
                        if (sucesso) aoSalvar(usuarioAtualizado)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                enabled = !salvando
            ) {
                Text(if (salvando) "Salvando..." else "Salvar Alterações", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}