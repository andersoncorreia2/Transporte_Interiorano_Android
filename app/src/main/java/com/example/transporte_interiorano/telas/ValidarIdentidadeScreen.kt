package com.example.transporte_interiorano.telas

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.ui.theme.AzulPrincipal

@Composable
fun ValidarIdentidadeScreen(
    onVoltar: () -> Unit,
    onValidacaoSucesso: () -> Unit,
    validarCpfTelefoneNoServidor: (String, String, (Boolean, String) -> Unit) -> Unit
) {
    val contexto = LocalContext.current
    var mostrarDialogoExplicao by remember { mutableStateOf(false) }
    var mostrarCamposValidacao by remember { mutableStateOf(false) }
    var cpfDigitado by remember { mutableStateOf("") }
    var telefoneDigitado by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Botão Voltar
            IconButton(onClick = onVoltar, modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Título Principal
            Text(
                text = "Selecione um método para confirmar sua identidade",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                lineHeight = 32.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtítulo descritivo
            Text(
                text = "Para solicitar uma viagem, confirme que você é o proprietário da conta. Não compartilharemos suas informações com os motoristas parceiros.",
                fontSize = 14.sp,
                color = Color.Gray,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Botão "Por que isso é necessário?"
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFF0F0F0),
                modifier = Modifier.clickable { mostrarDialogoExplicao = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Por que isso é necessário?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            if (!mostrarCamposValidacao) {
                // OPÇÃO 1: Confirmar com CPF e Telefone
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mostrarCamposValidacao = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = AzulPrincipal, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Confirmar com CPF e Telefone", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Use os dados cadastrados na sua conta para confirmação.", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(text = "›", fontSize = 24.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OPÇÃO 2: Cartão de Crédito (Simulado conforme referência Uber)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Toast.makeText(contexto, "Por favor, utilize a confirmação por CPF e Telefone.", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Cartão de crédito", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Somente para confirmação. Você ainda pode pagar em dinheiro ou pix.", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(text = "›", fontSize = 24.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // BLOCO DE DIGITAÇÃO DO CPF E TELEFONE
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(text = "Digite o CPF da sua conta:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = cpfDigitado,
                        onValueChange = { cpfDigitado = it },
                        placeholder = { Text("000.000.000-00") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Digite o Telefone com DDD:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = telefoneDigitado,
                        onValueChange = { telefoneDigitado = it },
                        placeholder = { Text("(00) 00000-0000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (carregando) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AzulPrincipal)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (cpfDigitado.isBlank() || telefoneDigitado.isBlank()) {
                                    Toast.makeText(contexto, "Preencha o CPF e o Telefone", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                carregando = true
                                validarCpfTelefoneNoServidor(cpfDigitado.trim(), telefoneDigitado.trim()) { sucesso, mensagem ->
                                    carregando = false
                                    Toast.makeText(contexto, mensagem, Toast.LENGTH_LONG).show()
                                    if (sucesso) {
                                        onValidacaoSucesso()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Confirmar Identidade 🔒", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoExplicao) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoExplicao = false },
            title = { Text("Segurança da Plataforma", fontWeight = FontWeight.Bold) },
            text = { Text("A confirmação de identidade é uma medida de segurança obrigatória para garantir que apenas titulares legítimos utilizem o serviço de transporte emergencial, prevenindo fraudes e protejendo motoristas e passageiros.") },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoExplicao = false }) {
                    Text("Entendi", color = AzulPrincipal, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}