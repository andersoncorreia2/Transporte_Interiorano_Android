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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(
    aoFazerLogin: (String, String) -> Unit,
    aoClicarCriarConta: () -> Unit,
    mensagemErro: String = ""
) {
    val context = LocalContext.current
    var usuarioInput by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .imePadding(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔹 BLOCO SUPERIOR: Título, Imagem e Subtítulo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.wrapContentHeight()
        ) {
            Text(
                "Transporte\nInteriorano",
                fontSize = 30.sp,
                color = AzulPrincipal,
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.veiculos),
                contentDescription = "Logo Transporte Interiorano",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(vertical = 4.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                "Viaje com Tranquilidade",
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }

        // 🔹 BLOCO DO MEIO: Campos de entrada de texto e Alertas
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false) // 🟢 Distribui o espaço do meio dinamicamente
                .padding(vertical = 4.dp)
        ) {
            if (mensagemErro.isNotEmpty()) {
                val corAlerta = if (mensagemErro.contains("Conectando")) AzulPrincipal else VermelhoErro
                Text(
                    mensagemErro,
                    color = corAlerta,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // 🟢 AJUSTADO AUTOMATICAMENTE: Altura compactada e densa com alinhamento interno centralizado perfeito
            OutlinedTextField(
                value = usuarioInput,
                onValueChange = { usuarioInput = it.filter { char -> !char.isWhitespace() } },
                label = { Text("Nome de Usuário") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 🟢 AJUSTADO AUTOMATICAMENTE: Altura compactada e densa com alinhamento interno centralizado perfeito
            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (senhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { senhaVisivel = !senhaVisivel }) { Icon(imageVector = image, contentDescription = null) }
                },
                shape = RoundedCornerShape(8.dp)
            )

            var mostrarDialogSenha by remember { mutableStateOf(false) }

            TextButton(
                onClick = { mostrarDialogSenha = true },
                modifier = Modifier.align(Alignment.End),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Esqueci minha senha", color = AzulPrincipal, fontWeight = FontWeight.Bold)
            }

            // --- [Lógica do Modal AlertDialog Mantida 100% Intacta] ---
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

                var novaSenhaVisivel by remember { mutableStateOf(false) }
                var confirmarSenhaVisivel by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { mostrarDialogSenha = false },
                    title = { Text(text = if (!emFaseDeValidacaoOtp) "Recuperar Senha" else "Confirmar Código OTP", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                        ) {
                            if (mensagemStatusModal.isNotEmpty()) {
                                Text(text = mensagemStatusModal, color = statusCorModal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            if (!emFaseDeValidacaoOtp) {
                                OutlinedTextField(value = emailRecup, onValueChange = { emailRecup = it; mensagemStatusModal = "" }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                                OutlinedTextField(value = cpfRecup, onValueChange = { novoTexto -> cpfRecup = novoTexto.filter { it.isDigit() }.take(11); mensagemStatusModal = "" }, label = { Text("CPF") }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = CpfVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            } else {
                                Text(text = "Insira o código de 6 dígitos enviado para o seu e-mail cadastrado.", color = Color.Gray, fontSize = 12.sp)
                                OutlinedTextField(value = codigoOtpRecup, onValueChange = { codigoOtpRecup = it.filter { char -> char.isDigit() }.take(6) }, label = { Text("Código de Verificação") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                OutlinedTextField(
                                    value = novaSenhaRecup,
                                    onValueChange = { novaSenhaRecup = it },
                                    label = { Text("Nova Senha") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    visualTransformation = if (novaSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        val image = if (novaSenhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                        IconButton(onClick = { novaSenhaVisivel = !novaSenhaVisivel }) { Icon(imageVector = image, contentDescription = null) }
                                    }
                                )

                                OutlinedTextField(
                                    value = confirmarSenhaRecup,
                                    onValueChange = { confirmarSenhaRecup = it },
                                    label = { Text("Confirmar Nova Senha") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    visualTransformation = if (confirmarSenhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        val image = if (confirmarSenhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                        IconButton(onClick = { confirmarSenhaVisivel = !confirmarSenhaVisivel }) { Icon(imageVector = image, contentDescription = null) }
                                    },
                                    isError = novaSenhaRecup.isNotEmpty() && confirmarSenhaRecup.isNotEmpty() && novaSenhaRecup != confirmarSenhaRecup,
                                    supportingText = { if (novaSenhaRecup.isNotEmpty() && confirmarSenhaRecup.isNotEmpty() && novaSenhaRecup != confirmarSenhaRecup) { Text("As senhas não coincidem!", color = VermelhoErro) } }
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
                                    val emailGarantido = emailRecup.trim().lowercase()
                                    val cpfGarantido = cpfRecup.filter { it.isDigit() }.trim()
                                    carregandoModal = true
                                    BancoDeDados.solicitarCodigoRecuperacao(emailGarantido, cpfGarantido) { sucesso, msg, _ ->
                                        carregandoModal = false
                                        if (sucesso) {
                                            emFaseDeValidacaoOtp = true
                                            mensagemStatusModal = msg
                                            statusCorModal = VerdeBotao
                                            codigoOtpRecup = ""
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
                                    val emailFinalRedefinir = emailRecup.trim().lowercase()
                                    carregandoModal = true
                                    BancoDeDados.redefinirSenhaComCodigo(emailFinalRedefinir, codigoOtpRecup.trim(), novaSenhaRecup) { sucesso, msg ->
                                        carregandoModal = false
                                        if (sucesso) {
                                            Toast.makeText(context, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show()
                                            mostrarDialogSenha = false
                                        } else {
                                            mensagemStatusModal = msg
                                            statusCorModal = VermelhoErro
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal), enabled = !carregandoModal
                        ) { Text(if (!emFaseDeValidacaoOtp) "Confirmar" else "Redefinir Senha") }
                    },
                    dismissButton = { TextButton(onClick = { mostrarDialogSenha = false }) { Text("Cancelar", color = VermelhoErro, fontWeight = FontWeight.Bold) } }
                )
            }
        }

        // 🔹 BLOCO INFERIOR: Botões fixos de ação (Espaço otimizado)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 🟢 CORRIGIDO: Botão "Entrar" aproximado e com altura redefinida para 42.dp
            Button(
                onClick = { aoFazerLogin(usuarioInput, senha) },
                colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) { Text("Entrar") }

            Spacer(modifier = Modifier.height(4.dp)) // 🟢 Reduzido espaçamento interno

            Text("Não possui conta? Crie a sua abaixo.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)

            // 🟢 CORRIGIDO: Espaçamento de linhas compactado para preencher duas linhas de forma justa
            Text(
                "Sua conta é permanente até que você decida excluí-la.",
                fontSize = 11.sp,
                color = AzulPrincipal,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            Spacer(modifier = Modifier.height(4.dp)) // 🟢 Reduzido espaçamento interno

            // 🟢 AJUSTADO: Altura do botão reduzida para 42.dp
            OutlinedButton(
                onClick = aoClicarCriarConta,
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) { Text("Criar conta", color = AzulPrincipal) }
        }
    }
}