package com.example.transporte_interiorano.telas

import com.example.transporte_interiorano.R
import com.example.transporte_interiorano.BancoDeDados
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.Usuario

@Composable
fun LoginScreen(
    aoFazerLogin: (String, String) -> Unit,
    aoClicarCriarConta: () -> Unit,
    mensagemErro: String = ""
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding() // 🆕 Isso faz o ajuste automático com o teclado
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🆕 AQUI ESTÁ A LOGO QUE VOCÊ PEDIU
        // Lembre-se de ter salvo a imagem como "logo_transporte" na pasta drawable


        Spacer(modifier = Modifier.height(16.dp)) // Inserido

        Text(
            "Transporte Interiorano",
            fontSize = 32.sp,
            color = AzulPrincipal,
            fontWeight = FontWeight.Bold
        )

        // 🆕 AQUI ENTRA A IMAGEM CENTRALIZADA
        Image(
            painter = painterResource(id = R.drawable.veiculos),
            contentDescription = "Logo Transporte Interiorano",
            modifier = Modifier.fillMaxWidth().height(180.dp), // Ajuste o tamanho conforme preferir
            contentScale = ContentScale.Fit
        )

        Text(
            "Viaje com Tranquilidade",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (mensagemErro.isNotEmpty()) {
            val corAlerta = if (mensagemErro.contains("Conectando")) AzulPrincipal else VermelhoErro
            Text(
                mensagemErro,
                color = corAlerta,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp),
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            singleLine = true, // 🆕 Impede que o campo cresça verticalmente
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        OutlinedTextField(
            value = senha, onValueChange = { senha = it }, label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth()
                .padding(bottom = 8.dp), // Reduzi o padding para caber o botão esqueci a senha
            visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image =
                    if (senhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                    Icon(
                        imageVector = image,
                        contentDescription = null
                    )
                }
            }
        )

        // 🆕 NOVO BOTÃO: Esqueci minha senha (Fluxo Blindado em Duas Etapas com OTP)
        var mostrarDialogSenha by remember { mutableStateOf(false) }

        TextButton(
            onClick = { mostrarDialogSenha = true },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Esqueci minha senha", color = AzulPrincipal, fontWeight = FontWeight.Bold)
        }

        if (mostrarDialogSenha) {
            var emailRecup by remember { mutableStateOf("") }
            var cpfRecup by remember { mutableStateOf("") }
            var codigoOtpRecup by remember { mutableStateOf("") }
            var novaSenhaRecup by remember { mutableStateOf("") }
            var confirmarSenhaRecup by remember { mutableStateOf("") }

            var emFaseDeValidacaoOtp by remember { mutableStateOf(false) }
            var mensagemStatusModal by remember { mutableStateOf("") }
            var statusCorModal by remember { mutableStateOf(AzulPrincipal) }
            var carregandoModal by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { mostrarDialogSenha = false },
                title = {
                    Text(
                        text = if (!emFaseDeValidacaoOtp) "Recuperar Senha" else "Confirmar Código OTP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (mensagemStatusModal.isNotEmpty()) {
                            Text(
                                text = mensagemStatusModal,
                                color = statusCorModal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (!emFaseDeValidacaoOtp) {
                            // --- PASSO 1: Coleta e validação de dados cadastrais ---
                            OutlinedTextField(
                                value = emailRecup,
                                onValueChange = { emailRecup = it; mensagemStatusModal = "" },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = cpfRecup,
                                onValueChange = { novoTexto ->
                                    val soNumeros = novoTexto.filter { it.isDigit() }.take(11)
                                    cpfRecup = soNumeros
                                    mensagemStatusModal = ""
                                },
                                label = { Text("CPF") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = CpfVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        } else {
                            // --- PASSO 2: Verificação do token OTP e nova credencial com dupla checagem ---
                            Text(
                                text = "Insira o código de 6 dígitos enviado para o seu e-mail cadastrado.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                            OutlinedTextField(
                                value = codigoOtpRecup,
                                onValueChange = { codigoOtpRecup = it.filter { char -> char.isDigit() }.take(6) },
                                label = { Text("Código de Verificação") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = novaSenhaRecup,
                                onValueChange = { novaSenhaRecup = it },
                                label = { Text("Nova Senha") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                            OutlinedTextField(
                                value = confirmarSenhaRecup,
                                onValueChange = { confirmarSenhaRecup = it },
                                label = { Text("Confirmar Nova Senha") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                isError = novaSenhaRecup.isNotEmpty() && confirmarSenhaRecup.isNotEmpty() && novaSenhaRecup != confirmarSenhaRecup,
                                supportingText = {
                                    if (novaSenhaRecup.isNotEmpty() && confirmarSenhaRecup.isNotEmpty() && novaSenhaRecup != confirmarSenhaRecup) {
                                        Text("As senhas não coincidem!", color = VermelhoErro)
                                    }
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!emFaseDeValidacaoOtp) {
                                if (emailRecup.isBlank() || cpfRecup.isBlank()) {
                                    mensagemStatusModal = "Preencha o Email e o CPF!"
                                    statusCorModal = VermelhoErro
                                    return@Button
                                }
                                carregandoModal = true
                                BancoDeDados.solicitarCodigoRecuperacao(emailRecup.trim(), cpfRecup) { sucesso, msg, codigoDebug ->
                                    carregandoModal = false
                                    if (sucesso) {
                                        emFaseDeValidacaoOtp = true
                                        mensagemStatusModal = msg
                                        statusCorModal = VerdeBotao

                                        if (codigoDebug != null) {
                                            codigoOtpRecup = codigoDebug  // Deixe ativo apenas durante os testes locais!
                                        }
                                    } else {
                                        mensagemStatusModal = msg
                                        statusCorModal = VermelhoErro
                                    }
                                }
                            } else {
                                if (codigoOtpRecup.length < 6 || novaSenhaRecup.isBlank()) {
                                    mensagemStatusModal = "Preencha o código e a nova senha!"
                                    statusCorModal = VermelhoErro
                                    return@Button
                                }
                                if (novaSenhaRecup != confirmarSenhaRecup) {
                                    mensagemStatusModal = "As senhas não coincidem!"
                                    statusCorModal = VermelhoErro
                                    return@Button
                                }
                                carregandoModal = true
                                BancoDeDados.redefinirSenhaComCodigo(emailRecup.trim(), codigoOtpRecup, novaSenhaRecup) { sucesso, msg ->
                                    carregandoModal = false
                                    if (sucesso) {
                                        mostrarDialogSenha = false // Fecha o diálogo com sucesso completo
                                    } else {
                                        mensagemStatusModal = msg
                                        statusCorModal = VermelhoErro
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                        enabled = !carregandoModal
                    ) {
                        Text(if (!emFaseDeValidacaoOtp) "Confirmar" else "Redefinir Senha")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogSenha = false }) {
                        Text("Cancelar", color = VermelhoErro, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { aoFazerLogin(email, senha) },
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Entrar")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Não possui conta? Crie a sua abaixo.", fontSize = 14.sp, color = Color.Gray)
        Text(
            "Sua conta é permanente até que você decida excluí-la.",
            fontSize = 12.sp,
            color = AzulPrincipal,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedButton(
            onClick = aoClicarCriarConta,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Criar conta", color = AzulPrincipal)
        }
    }
}