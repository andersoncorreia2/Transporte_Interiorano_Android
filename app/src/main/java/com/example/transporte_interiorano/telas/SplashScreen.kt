package com.example.transporte_interiorano.telas

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.ui.theme.AzulPrincipal
import kotlinx.coroutines.delay
import com.example.transporte_interiorano.dev.R
//import com.example.transporte_interiorano.R

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Executa um temporizador de 2.5 segundos em segundo plano antes de ir para o Login
    LaunchedEffect(Unit) {
        delay(2500)
        onTimeout()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Cor de fundo da sua tela de abertura
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 1. TEXTO DE CIMA CENTRALIZADO
        Text(
            text = "Transporte Interiorano",
            fontSize = 32.sp,
            color = AzulPrincipal,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. A SUA LOGO CENTRALIZADA
        Image(
            painter = painterResource(id = R.drawable.veiculos),
            contentDescription = "Logo Transporte Interiorano",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. TEXTO DE BAIXO EM 3 LINHAS CENTRALIZADAS
        Text(
            text = "Viaje com Segurança\ne\nTranquilidade",
            fontSize = 16.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}