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
import androidx.compose.ui.platform.LocalContext // Inserido
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast // Inserido
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
    var cpf by remember { mutableStateOf(usuarioAtual.cpf) }
    var telefone by remember { mutableStateOf(usuarioAtual.telefone) }
    var email by remember { mutableStateOf(usuarioAtual.email) }

    var cep by remember { mutableStateOf(usuarioAtual.cep) }
    var rua by remember { mutableStateOf(usuarioAtual.rua) }
    var numero by remember { mutableStateOf(usuarioAtual.numero) }
    var complemento by remember { mutableStateOf(usuarioAtual.complemento) }
    var bairro by remember { mutableStateOf(usuarioAtual.bairro) }
    var cidade by remember { mutableStateOf(usuarioAtual.cidade) }
    var estado by remember { mutableStateOf(usuarioAtual.estado) }

    var ofertarCarona by remember { mutableStateOf(usuarioAtual.veiculo.isNotEmpty()) }
    var veiculo by remember { mutableStateOf(usuarioAtual.veiculo) }
    var placa by remember { mutableStateOf(usuarioAtual.placa) }
    var vagas by remember { mutableStateOf(usuarioAtual.vagas) }

    var ufExpandido by remember { mutableStateOf(false) }
    val estadosBrasil = listOf("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO")
    var salvando by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Atualizar Dados", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = aoCancelar) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
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

            OutlinedTextField(
                value = nome, onValueChange = { nome = it }, label = { Text("Nome completo") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // 🆕 CPF adicionado com máscara (Bloqueado para edição para não quebrar o banco)
            OutlinedTextField(
                value = cpf, onValueChange = { }, label = { Text("CPF (Não editável)") }, modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                visualTransformation = CpfVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0xFFEEEEEE))
            )

            OutlinedTextField(
                value = telefone, onValueChange = { telefone = it.filter { char -> char.isDigit() }.take(11) }, label = { Text("Telefone / WhatsApp") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                visualTransformation = TelefoneVisualTransformation()
            )

            Text("ENDEREÇO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))

            // 1ª Linha: Rua e Nº
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rua, onValueChange = { rua = it }, label = { Text("Rua/Avenida") }, modifier = Modifier.weight(2f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = numero, onValueChange = { numero = it }, label = { Text("Nº") }, modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                )
            }

            // 2ª Linha: Complemento e Bairro
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = complemento, onValueChange = { complemento = it }, label = { Text("Compl. (Opcional)") }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = bairro, onValueChange = { bairro = it }, label = { Text("Bairro") }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
            }

            // 3ª Linha: Cidade e UF (Dropdown)
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = cidade, onValueChange = { cidade = it }, label = { Text("Cidade") }, modifier = Modifier.weight(1.3f), singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                ExposedDropdownMenuBox(
                    expanded = ufExpandido, onExpandedChange = { ufExpandido = !ufExpandido }, modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = estado, onValueChange = {}, readOnly = true, singleLine = true, label = { Text("UF") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ufExpandido) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = ufExpandido, onDismissRequest = { ufExpandido = false }) {
                        estadosBrasil.forEach { uf ->
                            DropdownMenuItem(text = { Text(uf) }, onClick = { estado = uf; ufExpandido = false })
                        }
                    }
                }
            }

            // 4ª Linha: CEP
            OutlinedTextField(
                value = cep, onValueChange = { cep = it.filter { char -> char.isDigit() }.take(8) }, label = { Text("CEP") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                visualTransformation = CepVisualTransformation()
            )

            Text("DADOS DA CONTA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))

            // 🆕 Email adicionado (Bloqueado para edição para não quebrar a chave do banco)
            OutlinedTextField(
                value = email, onValueChange = { }, label = { Text("Email (Não editável)") }, modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0xFFEEEEEE))
            )

            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = if (ofertarCarona) "Ofertar Corridas (Ativado)" else "Quero ofertar Corridas", color = if (ofertarCarona) AzulPrincipal else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Switch(checked = ofertarCarona, onCheckedChange = { ofertarCarona = it })
                    }

                    if (ofertarCarona) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = veiculo, onValueChange = { veiculo = it }, label = { Text("Modelo do Veículo") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = placa, onValueChange = { placa = it.uppercase().take(7) }, label = { Text("Placa") }, modifier = Modifier.weight(1.5f),
                                visualTransformation = PlacaVisualTransformation(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                            )
                            OutlinedTextField(value = vagas, onValueChange = { vagas = it }, label = { Text("Nº Vagas") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    salvando = true
                    val usuarioAtualizado = Usuario(
                        nome = nome, cpf = cpf, telefone = telefone, email = email, senha = usuarioAtual.senha,
                        veiculo = if (ofertarCarona) veiculo else "",
                        placa = if (ofertarCarona) placa else "",
                        vagas = if (ofertarCarona) vagas else "0",
                        rua = rua, numero = numero, complemento = complemento, bairro = bairro, cidade = cidade, estado = estado, cep = cep
                    )
                    BancoDeDados.atualizarUsuarioNuvem(usuarioAtualizado) { sucesso ->
                        if (sucesso) {
                            // 🆕 MOSTRA A MENSAGEM DE SUCESSO
                            Toast.makeText(context, "Atualização realizada com sucesso!", Toast.LENGTH_SHORT).show()

                            aoSalvar(usuarioAtualizado) // volta para o perfil
                        } else {
                            salvando = false
                            Toast.makeText(context, "Erro ao atualizar dados.", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = !salvando,
                colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (salvando) "Salvando..." else "Salvar Alterações", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}