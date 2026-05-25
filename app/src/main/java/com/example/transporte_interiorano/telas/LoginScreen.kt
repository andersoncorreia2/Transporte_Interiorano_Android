package com.example.transporte_interiorano.telas

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

@Composable
fun LoginScreen(
    aoFazerLogin: (String, String) -> Unit,
    aoClicarCriarConta: () -> Unit,
    mensagemErro: String = ""
) {
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }

    // VARIÁVEL QUE CONTROLA O OLHINHO DA SENHA
    var senhaVisivel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TÍTULO DO APP
        Text(
            text = "Tranporte Interiorano",
            fontSize = 32.sp,
            color = AzulPrincipal,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Viaje com Tranquilidade",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // MENSAGEM DE ERRO OU CARREGAMENTO
        if (mensagemErro.isNotEmpty()) {
            val corAlerta = if (mensagemErro.contains("Conectando")) AzulPrincipal else VermelhoErro
            Text(
                text = mensagemErro,
                color = corAlerta,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp),
                fontWeight = FontWeight.Bold
            )
        }

        // CAMPO DE EMAIL
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        // CAMPO DE SENHA COM O BOTÃO DE OLHINHO
        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (senhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (senhaVisivel) "Ocultar senha" else "Mostrar senha"

                IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

        // BOTÃO ENTRAR
        Button(
            onClick = { aoFazerLogin(email, senha) },
            colors = ButtonDefaults.buttonColors(containerColor = VerdeBotao),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Entrar")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🆕 INÍCIO DA ALTERAÇÃO: Adição dos textos informativos
        Text(
            text = "Não possui conta? Crie a sua abaixo.",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = "Sua conta é permanente até que você decida excluí-la.",
            fontSize = 12.sp,
            color = AzulPrincipal,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // 🆕 FIM DA ALTERAÇÃO

        // BOTÃO CRIAR CONTA
        OutlinedButton(
            onClick = aoClicarCriarConta,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Criar conta", color = AzulPrincipal)
        }
    }
}