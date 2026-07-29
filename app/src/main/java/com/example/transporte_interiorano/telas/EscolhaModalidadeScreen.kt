
package com.example.transporte_interiorano.telas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.transporte_interiorano.BancoDeDados
import com.example.transporte_interiorano.PagamentoEmergenteService
import com.example.transporte_interiorano.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EscolhaModalidadeScreen(
    onModalidadeSelecionada: (String) -> Unit,
    onClicarFecharGeral: () -> Unit,
    onClicarPerfil: () -> Unit,
    onClicarHistorico: () -> Unit,
    estaBloqueado: Boolean,
    verificandoBloqueio: Boolean,
    onQuitarDebito: () -> Unit
) {
    val contexto = LocalContext.current
    val escopoCorrotina = rememberCoroutineScope()

    // ESTADOS DO PIX DE DESBLOQUEIO DE R$ 0,01
    var carregandoPix by remember { mutableStateOf(false) }
    var pixCopiaColaCodigo by remember { mutableStateOf<String?>(null) }
    var qrCodeBase64Str by remember { mutableStateOf<String?>(null) }
    var valorCobradoPix by remember { mutableStateOf(0.01) }
    var corridaIdPendencia by remember { mutableStateOf<Int?>(null) }

    // POLING AUTOMÁTICO: Checa se o Webhook confirmou o pagamento no banco e desbloqueou o passageiro
    LaunchedEffect(estaBloqueado, pixCopiaColaCodigo) {
        if (estaBloqueado && pixCopiaColaCodigo != null) {
            while (estaBloqueado) {
                delay(3000)
                PagamentoEmergenteService.verificarDebitoPassageiro(BancoDeDados.tokenSessao) { bloqueadoAinda, _, _ ->
                    if (!bloqueadoAinda) {
                        Toast.makeText(contexto, "🎉 Pagamento Pix confirmado! Perfil desbloqueado.", Toast.LENGTH_LONG).show()
                        onQuitarDebito() // Atualiza o estado pai
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (verificandoBloqueio) {
            // 1. ESTADO DE CARREGAMENTO
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AzulPrincipal)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Verificando pendências financeiras...",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 40.dp)
                )
            }
        } else if (estaBloqueado) {
            // 2. TELA DE BLOQUEIO (Com suporte a Pix Real de R$ 0,01)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Débito Pendente 🚨",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (pixCopiaColaCodigo == null) {
                            Text(
                                text = "Identificamos uma corrida pendente. Gere o Pix de teste (R$ 0,01) para desbloquear o seu perfil automaticamente.",
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            if (carregandoPix) {
                                CircularProgressIndicator(color = AzulPrincipal)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Gerando Pix de R$ 0,01...", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                Button(
                                    onClick = {
                                        carregandoPix = true
                                        // Primeiro busca o ID da corrida devedora
                                        PagamentoEmergenteService.verificarDebitoPassageiro(BancoDeDados.tokenSessao) { _, _, detalhes ->
                                            val cId = detalhes?.optInt("corrida_id", 0) ?: 0
                                            corridaIdPendencia = cId

                                            // Chama a geração do Pix de R$ 0,01
                                            PagamentoEmergenteService.gerarPixDebitoPassageiro(cId, BancoDeDados.tokenSessao) { sucesso, msg, copiaCola, qrCodeB64, valor ->
                                                carregandoPix = false
                                                if (sucesso && copiaCola != null) {
                                                    pixCopiaColaCodigo = copiaCola
                                                    qrCodeBase64Str = qrCodeB64
                                                    valorCobradoPix = valor ?: 0.01
                                                } else {
                                                    Toast.makeText(contexto, msg, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) { Text("Gerar Pix de R$ 0.01 ⚡", fontWeight = FontWeight.Bold) }
                            }
                        } else {
                            // EXIBIÇÃO DO QR CODE E CÓDIGO PIX COPIA E COLA
                            Text(
                                text = "Valor de Teste: R$ ${String.format("%.2f", valorCobradoPix)}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // RENDERIZA A IMAGEM DO QR CODE BASE64 (FORA DO TRY-CATCH)
                            qrCodeBase64Str?.let { b64 ->
                                val bitmapParaExibir = remember(b64) {
                                    try {
                                        val imageBytes = Base64.decode(b64, Base64.DEFAULT)
                                        val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                        decodedImage?.asImageBitmap()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }
                                }

                                if (bitmapParaExibir != null) {
                                    Image(
                                        bitmap = bitmapParaExibir,
                                        contentDescription = "QR Code Pix",
                                        modifier = Modifier.size(180.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val clipboard = contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Pix Copia e Cola", pixCopiaColaCodigo)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(contexto, "Código Pix copiado! Cole no aplicativo do seu banco.", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) { Text("Copiar Código Pix 📋", fontWeight = FontWeight.Bold) }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aguardando confirmação do pagamento pelo banco...",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onClicarFecharGeral,
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) { Text("Depois (Sair)", color = Color.Gray) }
                    }
                }
            }
        } else {
            // 3. CONTEÚDO ORIGINAL
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    IconButton(onClick = onClicarFecharGeral) {
                        Icon(Icons.Default.Close, contentDescription = "Sair", tint = AzulPrincipal)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Como deseja viajar hoje?",
                        fontSize = 26.sp,
                        color = AzulPrincipal,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = onClicarPerfil,
                        colors = ButtonDefaults.buttonColors(containerColor = AzulPrincipal),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Ver Meu Perfil",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    OutlinedButton(
                        onClick = onClicarHistorico,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(size = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulPrincipal)
                    ) {
                        Text(
                            "Ver Histórico de Viagens",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // CARD 1: VIAGEM PROGRAMADA
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clickable {
                                PagamentoEmergenteService.alterarModalidadeUsuario("Programada", BancoDeDados.tokenSessao) { _ ->
                                    onModalidadeSelecionada("Programada")
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AzulPrincipal)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🗓️ Viagem Programada",
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Agende caronas para o interior ou eventos com antecedência.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // CARD 2: CORRIDA DE EMERGÊNCIA
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clickable {
                                PagamentoEmergenteService.alterarModalidadeUsuario("Emergencial", BancoDeDados.tokenSessao) { _ ->
                                    onModalidadeSelecionada("Emergencial")
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⚡ Corrida Emergencial",
                                fontSize = 20.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Precisa sair agora? Encontre motoristas nos bairros próximos.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}