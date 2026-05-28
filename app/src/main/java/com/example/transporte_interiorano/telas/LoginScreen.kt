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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale

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
        modifier = Modifier.fillMaxSize().padding(24.dp),
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
            value = email, onValueChange = { email = it }, label = { Text("Email") },
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

        // 🆕 NOVO BOTÃO: Esqueci minha senha
        var mostrarDialogSenha by remember { mutableStateOf(false) }

        TextButton(
            onClick = { mostrarDialogSenha = true },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Esqueci minha senha", color = AzulPrincipal, fontWeight = FontWeight.Bold)
        }

        if (mostrarDialogSenha) {
            var email by remember { mutableStateOf("") }
            var cpf by remember { mutableStateOf("") }
            var novaSenha by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { mostrarDialogSenha = false },
                title = { Text("Recuperar Senha") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") })
                        OutlinedTextField(
                            value = cpf,
                            onValueChange = { novoTexto ->
                                val soNumeros = novoTexto.filter { it.isDigit() }.take(11)
                                cpf = soNumeros
                            },
                            label = { Text("CPF") },
                            visualTransformation = CpfVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = novaSenha,
                            onValueChange = { novaSenha = it },
                            label = { Text("Nova Senha") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        BancoDeDados.recuperarSenhaNuvem(email, cpf, novaSenha) { sucesso, msg ->
                            if (sucesso) mostrarDialogSenha = false
                        }
                    }) {
                        Text("Confirmar")
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